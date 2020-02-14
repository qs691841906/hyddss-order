package com.sinosoft.ddss.service.impl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.sinosoft.ddss.common.constant.Constant;
import com.sinosoft.ddss.common.entity.Additional;
import com.sinosoft.ddss.common.entity.KeyValue;
import com.sinosoft.ddss.common.entity.OrderInfo;
import com.sinosoft.ddss.common.entity.OrderInfoMain;
import com.sinosoft.ddss.common.entity.User;
import com.sinosoft.ddss.common.entity.query.OrderInfoQuery;
import com.sinosoft.ddss.common.util.DateTimeUtils;
import com.sinosoft.ddss.config.ProducerConfiguration;
import com.sinosoft.ddss.dao.OrderInfoMainMapper;
import com.sinosoft.ddss.dao.OrderInfoMapper;
import com.sinosoft.ddss.dao.UserMapper;
import com.sinosoft.ddss.jedis.JedisClient;
import com.sinosoft.ddss.service.OrderMainService;
import com.sinosoft.ddss.utils.FastJsonUtil;

@Service
public class OrderMainServiceImpl implements OrderMainService {

	@Autowired
	private OrderInfoMainMapper orderMainMapper;
	@Autowired
	private OrderInfoMapper orderMapper;
	@Autowired
	private TaskServiceImpl taskServiceImpl;
	@Autowired
	private JedisClient jedisClient;
	@Autowired
	private UserMapper userMapper;
	
	final static ProducerConfiguration producer_OrderMainTaskPush = new ProducerConfiguration(Constant.E_ORDERMAIN_TASK_PUSH, Constant.QUE_ORDERMAIN_TASK_PUSH, Constant.QUE_ORDERMAIN_TASK_PUSH);
	final static ProducerConfiguration producer_DemandOrderMainTaskPush = new ProducerConfiguration(Constant.E_DEMANDORDERMAIN_TASK_PUSH, Constant.QUE_DEMANDORDERMAIN_TASK_PUSH, Constant.QUE_DEMANDORDERMAIN_TASK_PUSH);
	/**
	 * 查询主订单
	 */
	@Override
	public List<Additional> ListOrder(OrderInfoQuery orderInfo) {
		List<Additional> listOrder = orderMainMapper.listOrder(orderInfo);
		for(Additional additional:listOrder){
			User user = userMapper.checkUserName(additional.getUserName());
			
			additional.setRealName(user.getRealName());
		}
		
		
		String workTypes = jedisClient.get("workType");
		workTypes = workTypes.replace("\\", "");
		workTypes = workTypes.substring(1, workTypes.length()-1);
		// 将单位类别转换为对象
		List<KeyValue> list = FastJsonUtil.toList(workTypes, KeyValue.class);
		// 根据id判断当前单位类别
		for (Additional orderInfoMain : listOrder) {
			if(orderInfoMain.getWorkType()!=null){
				for (KeyValue keyValue : list) {
					if(Integer.parseInt(keyValue.getId())==orderInfoMain.getWorkType()){
						orderInfoMain.setWorkTypeStr(keyValue.getValue());
						break;
					}
				}
			}
//			如果订单状态是待审核就不显示完成订单数
			if(orderInfoMain.getAuditStatus()==1){
				orderInfoMain.setDowloadCount(0);
//				如果订单被关闭就显示关闭
				if(orderInfoMain.getOrderMainStatus()!=7){
					orderInfoMain.setOrderMainStatus((short) 1);
				}
			}
		}
		return listOrder;
	}

	/**
	 * 报警查询
	 */
	@Override
	public List<Additional> QueryCallPolice(Map<String, Object> orderInfo) {
		return orderMainMapper.QueryCallPolice(orderInfo);
	}

	/**
	 * 查询主订单数量
	 */
	@Override
	public Integer getCountByQuery(OrderInfoQuery orderInfo) {
		return orderMainMapper.getCountByQuery(orderInfo);
	}

	/**
	 * 修改订单状态
	 */
	@Override
	public Integer updateOrderStatus(Map<String, String> map) {
		return orderMainMapper.updateOrderStatus(map);
	}

	/**
	 * 审核订单
	 */
	@Override
	public boolean auditOrder(OrderInfoQuery orderInfoQuery) {
		// 将审核信息放到map中
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("auditStatus", orderInfoQuery.getAuditStatus());
		map.put("auditFailReason", orderInfoQuery.getAuditFailReason());
		map.put("orderMainId", orderInfoQuery.getOrderMainIds());
		map.put("auditor", orderInfoQuery.getAuditor());
		map.put("priority",orderInfoQuery.getPriority());
		
		
		
		// 审核订单
		Integer updateOrderStatus = orderMainMapper.audit(map);
		// 审核通过的订单加入任务表
		if (updateOrderStatus > 0) {
			// 根据主订单id查询主订单表内容
			String orderMainIds = orderInfoQuery.getOrderMainIds();
//			将审核通过的订单保存到mq
			Additional ordermain = new Additional();
//			ordermain.setOrderMainId(Long.valueOf(orderMainIds));
//			根据主订单号查询主订单信息
			ordermain = orderMainMapper.getOrderMainById(Long.valueOf(orderMainIds));
			//订单类型（1：普通订购,2：快速订购,3：普通定制,4：批量定制,5：普通采集,6：快速采集7:定制订购（混合）8.订阅）
			int order_type = ordermain.getOrderType();
			int auditStatus = ordermain.getAuditStatus();
//			只处理审核通过的订购的
			if(auditStatus==2){
				if(order_type==5){
	//				将审核通过的观测需求同步给内网
					producer_DemandOrderMainTaskPush.send(JSON.toJSONString(ordermain));
				}else{
//					将其他订单保存到任务表中
					producer_OrderMainTaskPush.send(JSON.toJSONString(ordermain));
				}
			}
			
			
//			List<OrderInfoMain> orderMainList = orderMainMapper.listOrderByOrderMainIds(orderMainIds);
//			for (int i = 0; i < orderMainList.size(); i++) {
//				taskServiceImpl.saveTaskByOrderMain(orderMainList.get(i));
//			}
		} else {
			return false;
		}
		return true;
	}

	/**
	 * 订单取消恢复
	 */
	@Override
	public Map<String, Object> cancelOrder(OrderInfoMain orderInfoMain) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String msg = "";
		boolean status = false;
		resultMap.put("status", status);
		// 取消恢复
		Short cancelStatus = orderInfoMain.getCancelStatus();
		if (cancelStatus == null) {
			msg = "cancelStatus null";
			resultMap.put("msg", msg);
			return resultMap;
		}
		if(cancelStatus==0){
			orderInfoMain.setOrderMainStatus((short) 6);
		}else{
			orderInfoMain.setOrderMainStatus((short) 1);
		}
		// 主订单号
		Long orderMainId = orderInfoMain.getOrderMainId();
		if (orderMainId == null) {
			msg = "orderMainId null";
			resultMap.put("msg", msg);
			return resultMap;
		}
		// 修改
		Integer updateOrderStatus = orderMainMapper.cancelOrder(orderInfoMain);
		if (updateOrderStatus >= 0) {
			resultMap.put("status", true);
			return resultMap;
		} else {
			msg = "update false ";
			resultMap.put("msg", msg);
			return resultMap;
		}
	}

	/**
	 * 逻辑删除订单
	 */
	@Override
	public Integer delOrder(String orderMainId) {
		return orderMainMapper.delOrder(orderMainId);
	}

	/**
	 * 生成订单序列 采集单：1 180323 01 000000 定制单：180323 000000 订购单：180323 000000
	 * 订购子单：180323 03 000000 定制子单：180323 02 000000
	 * 
	 * @param ordertype
	 * @return
	 */
	public Long getOrderIdNew(int ordertype) {
		Long order = (long) 0;
		String date = DateTimeUtils.getDateTime("yyMMdd");
		String pre = new String();
		String LSH = new String();
		Integer sequenceName = null;
		switch (ordertype) {
		case 1:// 定制主订单
		case 2:// 定制主订单
			LSH = "";
			sequenceName = 1;
			break;
		case 3:// 订购主订单
		case 4:// 订购主订单
			LSH = "";
			sequenceName = 1;
			break;
		case 5:// 采集单主订单
		case 6:// 采集单主订单
			pre = "1";
			LSH = "01";
			sequenceName = 1;
			break;
		case 7:// 定制子单
			LSH = "02";
			sequenceName = 2;
			break;
		case 8:// 订购子单
			LSH = "03";
			sequenceName = 2;
			break;
		default:
			break;
		}
		Long nextval = getHibernateSequence(sequenceName);
		DecimalFormat df = new DecimalFormat("000000");
		String str2 = df.format(nextval);

		String orders = pre + date + LSH + str2;
		order = Long.parseLong(orders);

		return order;
	}

	/**
	 * 查询订单序列
	 * 
	 * @param sequenceName
	 * @return
	 */
//	@Transactional(propagation=Propagation.REQUIRES_NEW,readOnly = true)
	public Long getHibernateSequence(final Integer sequenceName) {
		if (sequenceName == 1) {
			return orderMainMapper.getOrderMainSeq();
		} else if (sequenceName == 2) {
			return orderMapper.getOrderSeq();
		}
		return null;
	}

	/**
	 * 根据主订单id获取主单信息
	 */
	@Override
	public Additional getOrderMainById(Long orderMainId) {
		return orderMainMapper.getOrderMainById(orderMainId);
	}

	/**
	 * 修改订单信息
	 */
	@Override
	public boolean updateOderMainInfo(Additional orderMain) {
		Integer result = orderMainMapper.updateOrderMain(orderMain);
		return result > 0;
	}

	@Override
	public List<Map<String, Object>> findOrderByCond(Map<String, Object> condMap) {
		// TODO Auto-generated method stub
		List<Map<String, Object>> orderList = new ArrayList<Map<String, Object>>();
		String sonOrderIds = (String) condMap.get("sonOrderIds");// 主订单id,为多个时以","拼接
		if (null == sonOrderIds || "".equals(sonOrderIds.trim())) {// 当sonOrderIds为""或null时返回null
			return null;
		}
		String flag = (String) condMap.get("flag");
		// if ("1".equals(flag)) {// 1 订单 2 购物车
		orderList = orderMainMapper.findOrderBySonOrderId(sonOrderIds);
		String orderMainId;// 主订单id
		for (Map<String, Object> orderMain : orderList) {
			Integer orderMainType = (Integer) orderMain.get("order_type");
			// 订单类型（1：普通订购,2：快速订购,3：普通定制,4：批量定制,5：普通采集,6：快速采集7:定制订购（混合））
			switch (orderMainType) {
			case 1:
				orderMain.put("order_type", "普通订购");
				break;
			case 2:
				orderMain.put("order_type", "快速订购");
				break;
			case 3:
				orderMain.put("order_type", "普通定制");
				break;
			case 4:
				orderMain.put("order_type", "批量定制");
				break;
			case 5:
				orderMain.put("order_type", "普通采集");
				break;
			case 6:
				orderMain.put("order_type", "快速采集");
				break;
			case 7:
				orderMain.put("order_type", "定制订购");
				break;
			}
			// 主订单状态（1：待处理 ，2：处理中 ，3：规划中，4：采集中的5:完成 ，6：取消，7：失败）
			Integer orderMainStatus = (Integer) orderMain.get("order_main_status");
			switch (orderMainStatus) {
			case 1:
				orderMain.put("order_main_status", "待处理");
				break;
			case 2:
				orderMain.put("order_main_status", "处理中");
				break;
			case 3:
				orderMain.put("order_main_status", "规划中");
				break;
			case 4:
				orderMain.put("order_main_status", "采集中");
				break;
			case 5:
				orderMain.put("order_main_status", "已完成");
				break;
			case 6:
				orderMain.put("order_main_status", "已取消");
				break;
			case 7:
				orderMain.put("order_main_status", "失败");
				break;
			}
			// 分发方式（1：在线，2：专线，3：离线，4：光盘，5：U盘）
			Integer distributionType = (Integer) orderMain.get("distribution_type");
			switch (distributionType) {
			case 1:
				orderMain.put("distribution_type", "在线");
				break;
			case 2:
				orderMain.put("distribution_type", "专线");
				break;
			case 3:
				orderMain.put("distribution_type", "离线");
				break;
			case 4:
				orderMain.put("distribution_type", "离线-光盘");
				break;
			case 5:
				orderMain.put("distribution_type", "离线-U盘");
				break;
			}
			orderMainId = String.valueOf(orderMain.get("order_main_id"));// 获取主订单id
			condMap.put("orderMainId", orderMainId);
			List<Map<String, Object>> findSonOrderByCond = orderMapper.findSonOrderByCond(condMap);
			for (Map<String, Object> map : findSonOrderByCond) {
				Integer orderType = (Integer) map.get("order_type");
				// 订单类型（1：订购。2：订制，3：采集）
				switch (orderType) {
				case 1:
					map.put("order_type", "订购");
					break;
				case 2:
					map.put("order_type", "订制");
					break;
				case 3:
					map.put("order_type", "采集");
					break;
				}
				Integer orderStatus = (Integer) map.get("order_status");
				// 订单状态（1：待处理,2：处理中,3：待分发,4：已分发,5：取消,6：失败,）
				switch (orderStatus) {
				case 1:
					map.put("order_status", "待处理");
					break;
				case 2:
					map.put("order_status", "处理中");
					break;
				case 3:
					map.put("order_status", "待分发");
					break;
				case 4:
					map.put("order_status", "已分发");
					break;
				case 5:
					map.put("order_status", "取消");
					break;
				case 6:
					map.put("order_status", "失败");
					break;
				}
				Integer downloadStatus = (Integer) map.get("download_status");
				// 下载状态（1：等待连接, 2：等待下载，3：已下载，4：已失效 ）
				switch (downloadStatus) {
				case 1:
					map.put("download_status", "等待连接");
					break;
				case 2:
					map.put("download_status", "等待下载");
					break;
				case 3:
					map.put("download_status", "已下载");
					break;
				case 4:
					map.put("download_status", "已失效");
					break;
				}
				map.put("distribution_status", orderMain.get("distribution_type"));
			}
			orderMain.put(orderMainId, findSonOrderByCond);
		}
		// }
		return orderList;
	}

	/**
	 * 根据状态统计主单数量(定制订购)
	 */
	@Override
	public List<OrderInfoQuery> statisticalOrderMainByStatus(OrderInfoQuery orderInfoQuery) {
		List<OrderInfoQuery> resule = orderMainMapper.statisticalOrderMainByStatus(orderInfoQuery);
		return resule;
	}

	/**
	 * 根据状态统计主单数量（采集）
	 */
	@Override
	public List<OrderInfoQuery> statisticalCollectinfoByStatus(OrderInfoQuery orderInfoQuery) {
		orderInfoQuery.setOrderType((short) 8);
		List<OrderInfoQuery> resule = orderMainMapper.statisticalOrderMainByStatus(orderInfoQuery);
		return resule;
	}

	/**
	 * 封装orderInfoQuery
	 * 
	 * @param request
	 * @return
	 */
	public OrderInfoQuery packagingOrderInfoQuery(OrderInfoQuery orderInfoQuery) {
		return orderInfoQuery;
	}
//	@PostConstruct
	public void updateOrderInfoMainStatus(){
		
//	查询处理中的订单
		OrderInfoMain orderInfoMain = new OrderInfoMain();
		orderInfoMain.setOrderMainStatus(Short.valueOf("2"));
		orderInfoMain.setOrderType(Short.valueOf("1"));
		List<Additional> orderInfoMain_list = orderMainMapper.dopDemand(orderInfoMain);
		if(orderInfoMain_list!=null||orderInfoMain_list.size()>0){
			
//	将完结的订单状态修改为5.完成
			for(Additional additional:orderInfoMain_list){
				Long orderMainId = additional.getOrderMainId();
//		根据主订单好查询子订单
				List<OrderInfo> orderInfo_list = orderMapper.selectByOrderMainId(orderMainId);
				if(orderInfo_list==null||orderInfo_list.size()==0){
					continue;
				}
				int i = 0;
				int j = orderInfo_list.size();
//		循环比较订单是否完结
				for(OrderInfo orderInfo:orderInfo_list){
					Short status = orderInfo.getOrderStatus();
					if(status==(short)3||status==(short)4||status==(short)7){
						i = i+1;
					}
				}
//		如果订单完结就将主单改为5完成
				if(i>=j){
					OrderInfoMain orderInfoMain_p = new OrderInfoMain();
					orderInfoMain_p.setOrderMainId(orderMainId);
					orderInfoMain_p.setOrderMainStatus(Short.valueOf("5"));
					orderMainMapper.updateByPrimaryKeySelective(orderInfoMain_p);
				}
				
			}
		}
	}

}
