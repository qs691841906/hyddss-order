package com.sinosoft.ddss.service.impl;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
//import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.sinosoft.ddss.common.entity.Additional;
import com.sinosoft.ddss.common.entity.Dtplan;
import com.sinosoft.ddss.common.entity.EmailTask;
import com.sinosoft.ddss.common.entity.KeyValue;
import com.sinosoft.ddss.common.entity.Metadata;
import com.sinosoft.ddss.common.entity.OrderInfo;
import com.sinosoft.ddss.common.entity.Role;
import com.sinosoft.ddss.common.entity.ShopInfo;
import com.sinosoft.ddss.common.entity.User;
import com.sinosoft.ddss.common.entity.query.OrderInfoQuery;
import com.sinosoft.ddss.common.util.JsonUtils;
import com.sinosoft.ddss.dao.DtplanMapper;
import com.sinosoft.ddss.dao.EmailTaskMapper;
import com.sinosoft.ddss.dao.OrderInfoMainMapper;
import com.sinosoft.ddss.dao.OrderInfoMapper;
import com.sinosoft.ddss.dao.RoleMapper;
import com.sinosoft.ddss.dao.ShopInfoMapper;
import com.sinosoft.ddss.dao.UserMapper;
import com.sinosoft.ddss.dataDao.ddssMetadataMapper;
import com.sinosoft.ddss.jedis.JedisClient;
import com.sinosoft.ddss.service.DecryptToken;
import com.sinosoft.ddss.service.OrderMainService;
import com.sinosoft.ddss.service.OrderService;
import com.sinosoft.ddss.service.ShopCarService;
import com.sinosoft.ddss.service.TaskService;
import com.sinosoft.ddss.utils.CreateEmailUtils;
import com.sinosoft.ddss.utils.FastJsonUtil;
import com.sinosoft.ddss.utils.HttpClient;

import net.sf.json.JSONObject;


@Service
public class OrderServiceImpl implements OrderService {

	private static Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
	@Autowired
	private OrderMainService orderMainService;
	@Autowired
	private OrderInfoMainMapper orderMainMapper;
	@Autowired
	private RoleMapper roleMapper;
	@Autowired
	private OrderInfoMapper orderMapper;
	@Autowired
	private JedisClient jedisClient;
	@Autowired
	private ddssMetadataMapper metadataQueryMapper;
	@Autowired
	private TaskService taskService;
	@Autowired
	private ShopInfoMapper shopInfoMapper;
	@Autowired
	private ShopCarService shopCarService;
	@Autowired
	private DtplanMapper dtplanMapper;
	@Autowired
	private DecryptToken decryptToken;
	@Autowired
	private EmailTaskMapper emailTaskMapper;
	@Autowired
	private UserMapper userMapper;

	/**
	 * 查询订单数
	 */
	@Override
	public Integer getOrderCountByQuery(OrderInfoQuery orderInfo) {
		return orderMapper.getOrderCountByQuery(orderInfo);
	}

	/**
	 * 购物车生成订单
	 */
	@Override
	public boolean saveOrderByShopCar(Additional additional) {
		// 获取购物车id
		String shopCarIds = additional.getShopCarIds();
		List<ShopInfo> ShopInfoList = shopInfoMapper.selectByPrimaryKeys(shopCarIds);
		// 遍历购物车数据，判断订单类型（全部是1 就是普通订购，全部是3就是普通定制，否则是7定制订购） 购物车订单类型（1：定制，2：订购）
		short orderType = 0;
		for (ShopInfo shopInfo : ShopInfoList) {
			if (orderType == 0) {
				orderType = shopInfo.getOrderType();
			} else if (orderType == shopInfo.getOrderType()) {
				continue;
			} else {
				orderType = 7;
				break;
			}
		}
		if(orderType == 1){
			orderType = 3;
		}else if(orderType == 2){
			orderType = 1;
		}
//		orderType = orderType == 2 ? (short) 1 : (short) 3;
		// 设置订单类型，生成订单
		additional.setOrderType(orderType);
		boolean saveOrder = saveOrder(additional);
		if (saveOrder) {
			shopCarService.deleteByPrimaryKey(shopCarIds);
		}
		return true;
	}

	/**
	 * 新增订单
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean saveOrder(Additional additional) {
		// 订单类型
		short orderType = additional.getOrderType();

		// 跟令牌查询个人信息
		String token = additional.getToken();
		User user = decryptToken.decyptToken(token);
		if (user == null) {
			return false;
		}
		Role ro = roleMapper.selectByPrimaryKey(user.getRoleId());
		Additional orderInfoMain = new Additional();
		// 订单类型
		orderInfoMain.setOrderType(orderType);
		// 订单名称
		orderInfoMain.setOrderName(additional.getOrderName());
		// 订单描述
		if (!StringUtils.isBlank(additional.getOrderMainDesc())) {
			orderInfoMain.setOrderMainDesc(additional.getOrderMainDesc());
		}
//		orderInfoMain.setOrderMainDesc(
//				StringUtils.isBlank(additional.getOrderMainDesc()) ? additional.getOrderMainDesc() : "");
		// 根据订单类型生成订单id
		Long orderMainId = orderMainService.getOrderIdNew(orderType);
		orderInfoMain.setOrderMainId(orderMainId);
		// 分发方式
		orderInfoMain.setDistributionType(additional.getDistributionType());
		//U盘光盘CDROMUdisk
//		if (additional.getDistributionType().equals((short)2)) {
//			orderInfoMain.setcdromudisk(additional.getcdromudisk());
//		}
		// 获取当前用户信息
		orderInfoMain.setUserName(user.getUserName());
		orderInfoMain.setWorkType(user.getWorkType());
		orderInfoMain.setUserUnit(user.getWorkUnit());
		List<OrderInfo> orderList = new ArrayList<OrderInfo>();
		// 订单状态初始为待处理（1：待处理 ，2：处理中 ，3：规划中，4：采集中的5:完成 ，6：取消，7：关闭，8：失败）
		orderInfoMain.setOrderMainStatus((short) 1);
		// 是否同步分发状态初始定义未分发（0：未分发，1：已分发）
		orderInfoMain.setDistributionStatus((short) 0);
		// 是否删除
		orderInfoMain.setIsDel((short) 1);
		// 取消恢复状态（0：取消状态,1：恢复状态）
		orderInfoMain.setCancelStatus((short) 1);
		// 查看是否需要自动审核
		String orderAutomaticReview = jedisClient.get("orderAutomaticReview");
		// TODO 订单优先级
		if (StringUtils.isBlank(orderAutomaticReview)) {
			orderAutomaticReview = "0";
		}
		// 1：普通订购,2：快速订购,3：普通定制,4：批量定制,5：普通采集单,6：快速采集，7：定制订购
		if (orderType == 1 || orderType == 3 || orderType == 7) {
			// ******************************************
			// 判断dataIds是有值，有的话就是查询页面跳过来的
			// dataIds没有值，就是购物车跳过来的，获取shopCarIds
			// ******************************************
			// 获取数据id
			String dataIds = additional.getDataIds();
			Map<Long, OrderInfo> map = new HashMap<Long, OrderInfo>();
			if (StringUtils.isBlank(dataIds)) {
				dataIds = "";
				// 获取购物车id
				String shopCarIds = additional.getShopCarIds();
				// 查询购物车信息
				List<ShopInfo> ShopInfoList = shopInfoMapper.selectByPrimaryKeys(shopCarIds);
				// 遍历购物车信息，定义子单信息
				for (ShopInfo shopInfo : ShopInfoList) {
					OrderInfo orderInfo = new OrderInfo();
					orderInfo.setOutProductLevel(shopInfo.getOutProductLevel());
					orderInfo.setOrderType(shopInfo.getOrderType());
					orderInfo.setImageStartTime(shopInfo.getImageStartTime());
					orderInfo.setImageEndTime(shopInfo.getImageEndTime());
					map.put(shopInfo.getDataId(), orderInfo);
					dataIds += shopInfo.getDataId() + ",";
				}
				if (StringUtils.isBlank(dataIds)) {
					return false;
				} else {
					dataIds = dataIds.substring(0, dataIds.length() - 1);
				}
			}
			// dataIds查询数据封装将要生成的自订单信息
			if (StringUtils.isBlank(dataIds)) {
				return false;
			}
			List<Metadata> listDatas = metadataQueryMapper.listDatas(dataIds);
			// 子单个数
			orderInfoMain.setRealOrderCount(listDatas.size());
			// 是否自动审核(订购，在线，该当天数据量，当前数据量)
			boolean automaticReview = false;
			if(orderAutomaticReview.equals("1")){
				// 普通订购
				if(orderType == 1){
					// 在线
					if(additional.getDistributionType() == 1){
						// 判断当前数据量
						double sumDataSize = 0.0;
						for (Metadata metadata : listDatas) {
							sumDataSize += metadata.getDataSize();
						}
						// 单个订单数据量限制
//						String OrderDataSizeLimit = jedisClient.get("OrderDataSizeLimit");
						String OrderDataSizeLimit = String.valueOf(ro.getOrderMaxSize()==null?0:ro.getOrderMaxSize());
						if(Double.parseDouble(OrderDataSizeLimit)*1024*1024 >= sumDataSize){
							// 判断该用户当天下单数据量
//							String UserDataSizeEveryDayLimit = jedisClient.get("UserDataSizeEveryDayLimit");
							String UserDataSizeEveryDayLimit = String.valueOf(ro.getDayMaxSize()==null?0:ro.getDayMaxSize());
							// 查询该用户当前下载数据量
							Double todayDataSize = orderMainMapper.sumDataSizeToday(user.getUserName());
							if(todayDataSize==null)todayDataSize=0.0;
							if(Double.parseDouble(UserDataSizeEveryDayLimit)*1024*1024 >= todayDataSize){
								// 自动审核通过
								orderInfoMain.setAuditStatus((short) 2);
								Date now = new Date();
								SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
								String nowTime = sdf.format(now);
								orderInfoMain.setAuditTime(nowTime);
								automaticReview = true;
							}
						}
					}
				}
			}else{
				orderInfoMain.setAuditStatus((short) 1);
			}
			if(!automaticReview){
				orderInfoMain.setAuditStatus((short) 1);
			}
//			orderInfoMain.setAuditStatus(orderAutomaticReview.equals("1") ? (short) 2 : (short) 1);
			// 生成订单
			orderMainMapper.saveOrderMain(orderInfoMain);
			if (listDatas != null) {
				for (Metadata metadata : listDatas) {
					// 定义子单信息
					OrderInfo orderInfo = new OrderInfo();
					// 获取子单id
					Long orderInfoId = 0L;
					if (orderType == 1 || orderType == 7) {
						orderInfoId = orderMainService.getOrderIdNew(8);
					} else {
						orderInfoId = orderMainService.getOrderIdNew(7);
					}
					orderInfo.setOrderId(orderInfoId);
					// 定义主单id
					orderInfo.setOrderMainId(orderMainId);
					// TODO 优先级
					String priority = jedisClient.get("collPriority");
					orderInfo.setPriority(Short.parseShort(priority));
					
					orderInfo.setDataId(metadata.getDataId());
					
					orderInfo.setImageStartTime(metadata.getImageStartTime());
					orderInfo.setImageEndTime(metadata.getImageEndTime());
					
					// 数据id
					orderInfo.setDataId(metadata.getDataId());
					// 产品级别(定制有输出级别)
					// 定义任务类型(1:订购2:定制7:定制订购)
					String productLevel = metadata.getProductLevel();
					orderInfo.setInProductLevel(productLevel);
					if (orderType == 1) {
						orderInfo.setOutProductLevel(metadata.getProductLevel());
						orderInfo.setOrderType((short) 1);
					} else if (orderType == 3) {
						orderInfo.setOutProductLevel(additional.getOutProductLevel());
						orderInfo.setOrderType((short) 2);
					} else if (orderType == 7) {
						orderInfo.setOutProductLevel(map.get(metadata.getDataId()).getOutProductLevel());
						orderInfo.setOrderType(map.get(metadata.getDataId()).getOrderType());
					}
					// 卫星
					String satellite = metadata.getSatellite();
					orderInfo.setSatellite(satellite);
					// 传感器
					String sensor = metadata.getSensor();
					orderInfo.setSensor(sensor);
					// 云量
					orderInfo.setInProductLevel(metadata.getProductLevel());
					// 产品名称
					orderInfo.setProductName(metadata.getProductName());
					// 数据大小
					orderInfo.setDataSize(metadata.getDataSize());
					// 订单状态 1：待处理,2：处理中,3：待分发,4：已分发,5：取消,6：关闭,7：失败
					orderInfo.setOrderStatus((short) 1);
					// 下载状态 1：等待连接,2：等待下载,3：已下载,4：已失效
					orderInfo.setDownloadStatus((short) 1);
					// 订单分发状态 0：生产完成，1：推送完成，2：入库完成
					orderInfo.setDistributionStatus((short) 1);
					// 取消恢复 0：取消状态,1：恢复状态
					orderInfo.setCancelStatus((short) 1);
					// 用户名
					orderInfo.setUserName(user.getUserName());
					// 用户单位
					orderInfo.setUserUnit(user.getWorkUnit());
					// 单位类别
					Integer workType = user.getWorkType();
//					String workTypeStr = "";
//					// 从配置中读取单位类别
//					String workTypes = jedisClient.get("workType");
//					workTypes = workTypes.replace("\\", "");
//					workTypes = workTypes.substring(1, workTypes.length()-1);
//					// 将单位类别转换为对象
//					List<KeyValue> list = FastJsonUtil.toList(workTypes, KeyValue.class);
//					// 根据id判断当前单位类别
//					for (KeyValue keyValue : list) {
//						if(Integer.parseInt(keyValue.getId())==workType){
//							workTypeStr = keyValue.getValue();
//							break;
//						}
//					}
					orderInfo.setWorkType(workType);
					// 产品下载地址
					orderInfo.setProDownloadAdd("/" + satellite + "/" + sensor + "/" + productLevel + "/");
					//ftp url ftp://username:password@ip:port
//					orderInfo.setFtpUrl("ftp://"+user.getUserName()+":"+user.getUserPwd()+"@"+jedisClient.get("ftpIp")+":"+jedisClient.get("ftpPort"));
					orderList.add(orderInfo);
				}
			}
			// 批量添加子订单
			orderMapper.saveOrder(orderList);
		} else if (orderType == 2 || orderType == 4 || orderType == 5 || orderType == 6 || orderType == 8) {
			
	        String url = "http://172.16.25.27:8765/oauth/map_query_sensor";
	        Map<String, String> map = new HashMap<String, String>();
	        
	        map.put("pageSize", "1000");
	        map.put("timeType", "image_start_time");
	        map.put("startTime", additional.getStartTime());
	        map.put("endTime", additional.getEndTime());
	        map.put("satellite", additional.getSatellite());
	        map.put("sensor", additional.getSensor());
	        map.put("outProductLevel", "L1");
	        map.put("ploygon", additional.getAoi());
	        map.put("regionalType", "2");
	        map.put("token", additional.getToken());

	        List<Metadata> dataList = new ArrayList<Metadata>();
	        
	        try {
//				String body = HttpClient.sendPostDataByJson(url, JSON.toJSONString(map), "utf-8");
	        	String body = HttpClient.sendPostDataByMap(url, map, "utf-8");
	        	
	        	
	        	 JSONObject jsonObject = JSONObject.fromObject(body);
	             Map<Object, Object> map2 = (Map)jsonObject;
	            String data = String.valueOf(map2.get("data"));
	        	
	        	Metadata metadata = new Metadata();
	        	
	        	dataList= (List<Metadata>) JSON.parseArray(data, metadata.getClass());
	        	
				System.out.println("响应结果：" + body);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(null!=dataList&&dataList.size()>0){

				for (Metadata metadata : dataList) {
					// 定义子单信息
					OrderInfo orderInfo = new OrderInfo();
					// 获取子单id
					Long orderInfoId = 0L;
					if (orderType == 1 || orderType == 7) {
						orderInfoId = orderMainService.getOrderIdNew(8);
					} else {
						orderInfoId = orderMainService.getOrderIdNew(7);
					}
					orderInfo.setOrderId(orderInfoId);
					// 定义主单id
					orderInfo.setOrderMainId(orderMainId);
					// TODO 优先级
					orderInfo.setDataId(metadata.getDataId());
					
					orderInfo.setImageStartTime(metadata.getImageStartTime());
					orderInfo.setImageEndTime(metadata.getImageEndTime());
					
					// 数据id
					orderInfo.setDataId(metadata.getDataId());
					// 产品级别(定制有输出级别)
					// 定义任务类型(1:订购2:定制7:定制订购)
					String productLevel = metadata.getProductLevel();
					orderInfo.setInProductLevel(productLevel);
					orderInfo.setOutProductLevel(additional.getOutProductLevel());
					orderInfo.setOrderType((short) 2);
					// 卫星
					String satellite = metadata.getSatellite();
					orderInfo.setSatellite(satellite);
					// 传感器
					String sensor = metadata.getSensor();
					orderInfo.setSensor(sensor);
					// 云量
					orderInfo.setInProductLevel(metadata.getProductLevel());
					// 产品名称
					orderInfo.setProductName(metadata.getProductName());
					// 数据大小
					orderInfo.setDataSize(metadata.getDataSize());
					// 订单状态 1：待处理,2：处理中,3：待分发,4：已分发,5：取消,6：关闭,7：失败
					orderInfo.setOrderStatus((short) 1);
					// 下载状态 1：等待连接,2：等待下载,3：已下载,4：已失效
					orderInfo.setDownloadStatus((short) 1);
					// 订单分发状态 0：生产完成，1：推送完成，2：入库完成
					orderInfo.setDistributionStatus((short) 1);
					// 取消恢复 0：取消状态,1：恢复状态
					orderInfo.setCancelStatus((short) 1);
					// 用户名
					orderInfo.setUserName(user.getUserName());
					// 用户单位
					orderInfo.setUserUnit(user.getWorkUnit());
					// 单位类别
					Integer workType = user.getWorkType();
					orderInfo.setWorkType(workType);
					// 产品下载地址
					orderInfo.setProDownloadAdd("/" + satellite + "/" + sensor + "/" + productLevel + "/");
					orderList.add(orderInfo);
				}
				orderMapper.saveOrder(orderList);
			}
			
			// 子单个数
			orderInfoMain.setRealOrderCount(dataList.size());
			// 卫星
			orderInfoMain.setSatellite(additional.getSatellite());
			// 传感器
			orderInfoMain.setSensor(additional.getSensor());
			// 开始时间
			orderInfoMain.setStartTime(additional.getStartTime());
			// 结束时间
			orderInfoMain.setEndTime(additional.getEndTime());
			// 输入产品级别
			orderInfoMain.setInProductLevel(additional.getInProductLevel());
			// 输出产品级别
			orderInfoMain.setOutProductLevel(additional.getOutProductLevel());
			// 紧急程度
			// orderInfoMain.setUrgentLevel();
			// AOI
			orderInfoMain.setAoi(additional.getAoi());
			// 观测模式
			orderInfoMain.setWorkMode(additional.getWorkMode());
			// 预计子订单数
			orderInfoMain.setPredictOrderCount(dataList.size());
			// 采集单审核方式:1=自动，0=手动
			String collAutomaticReview = jedisClient.get("collAutomaticReview");
			if(StringUtils.isBlank(collAutomaticReview)){
				collAutomaticReview = "0";
			}
			if("0".equals(collAutomaticReview)){
				orderInfoMain.setAuditStatus((short)1);
				String priority = jedisClient.get("collPriority");
				if("".equals(priority)||null==priority){
					priority = "5";
				}
				orderInfoMain.setPriority(Integer.parseInt(priority));
				
			}else if("1".equals(collAutomaticReview)){
				orderInfoMain.setAuditStatus((short)2);
			}
			if(orderType == 8){
				orderInfoMain.setAuditStatus((short)2);
			}
			
			
			// 生成订单
			orderMainMapper.saveOrderMain(orderInfoMain);
		}
		// 如果是自动审核就要过滤然后加入任务表
		if (orderInfoMain.getAuditStatus() == 2) {
			// 过滤（普通定制与普通订购要过滤）
			//过滤暂时注掉
			/*if (orderType == 1 || orderType == 3 || orderType == 7) {
				OrderInfoQuery filterOrderInfo = new OrderInfoQuery();
				filterOrderInfo.setOrderMainId(orderMainId);
				filterOrderInfo.setOutProductLevel(additional.getOutProductLevel());
				FilterOrder.filter(filterOrderInfo);
			} else {
				// 添加任务表
				taskService.saveTaskByOrderMain(orderInfoMain);
			}*/
			taskService.saveTaskByOrderMain(orderInfoMain);
		}
		//插入邮件任务表
		String msg = String.format(jedisClient.get("orderGenerateContent"), orderInfoMain.getOrderMainId());
		EmailTask emailTask = CreateEmailUtils.createEmailTask(jedisClient.get("ddssSender"), msg, user.getUserName(), user.getUserEmail(), jedisClient.get("orderGenerateSubject"));
		emailTaskMapper.insertSelective(emailTask);
		return true;
	}

	/**
	 * 查询页面跳转生成订单
	 * 
	 * @param request
	 * @return
	 */
	@Override
	public boolean toOrderCreate(Additional additional) {
		return saveOrder(additional);
	}

	/**
	 * 根据主单id查询子单
	 */
	@Override
	public List<OrderInfo> getOrderInfoByOrderMainId(OrderInfoQuery orderInfoQuery) {
		List<OrderInfo> orderList = orderMapper.getOrderInfoByOrderMainId(orderInfoQuery);
		// 拼接下载路径
		String ftpIP = jedisClient.get("ftpIp");
		String ftpPort = jedisClient.get("ftpPort");
		// 获取当前订单的用户
		User userByName = null;
		if(orderList.size()>0){
			String userName = orderList.get(0).getUserName();
			User user = new User();
			user.setUserName(userName);
			userByName = userMapper.getUserByName(user);
//			String password = userByName.getPassword();
		}
		// 解析订单跟踪生成map
		for (int i = 0; i < orderList.size(); i++) {
			orderList.get(i).setFtpUrl("ftp://"+userByName.getUserName()+":"+userByName.getUserPwd()+"@"+ftpIP+":"+ftpPort);
			OrderInfo orderInfo = orderList.get(i);
			String orderStep = orderInfo.getOrderStep();
			String[] orderStepList = orderStep.split(",");
			if (orderStepList.length > 0) {
				for (int j = 0; j < orderStepList.length; j++) {
					Map<String, String> otderStepMap = new HashMap<String, String>();
					String[] stepTimes = orderStepList[j].split("_");
					otderStepMap.put("statu", stepTimes[0]);
					otderStepMap.put("time", stepTimes[1]);
					orderInfo.setOrderStepMap(otderStepMap);
				}
				orderList.set(i, orderInfo);
			}
		}
		return orderList;
	}

	/**
	 * 统计各个状态的订单数量
	 * 
	 * @return
	 */
	@Override
	public List<OrderInfoQuery> statisticalOrderByStatus(OrderInfoQuery orderInfoQuery) {
		List<OrderInfoQuery> orderList = orderMapper.statisticalOrderByStatus(orderInfoQuery);
		return orderList;
	}

	/**
	 * 更新订单状态
	 * 
	 * @return
	 */
	@Override
	public Integer updateOrderStatus(Map<String, String> params) throws Exception {
		return orderMapper.updateOrderStatus(params);

	}

	/**
	 * 查询子单详情
	 */
	@Override
	public OrderInfo getOrderInfoById(Long orderId) {
		return orderMapper.getOrderInfoById(orderId);
	}

	/**
	 * 查询采集单子单详情
	 */
	@Override
	public List<Map<String, Object>> getOrderInfoByCollectionId(OrderInfoQuery orderInfoQuery) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
		// 查询数传计划跟完备信息
		List<Dtplan> dtPlans = dtplanMapper.getDtplanByOrderMainId(orderInfoQuery.getOrderMainId());
		if (dtPlans == null || dtPlans.size() == 0) {
			return null;
		}
		orderInfoQuery.setStartNum(null);
		// 循环，将批次号生成
		String batchNos = "";
		for (int i = (orderInfoQuery.getPage() - 1) * orderInfoQuery.getPageSize() + 1; i <= orderInfoQuery
				.getPageSize(); i++) {
			batchNos += " or batch_no =" + i + " ";
		}
		if (batchNos.length() > 0) {
			batchNos = "(" + batchNos.substring(3) + ")";
		}
		orderInfoQuery.setBatchNos(batchNos);
		// 查出所在批次的子单信息
		List<OrderInfo> orderInfoList = orderMapper.getOrderInfoByOrderMainId(orderInfoQuery);
		// 将子单信息与数传计划匹配放在map
		for (int i = 0; i < dtPlans.size(); i++) {
			Map<String, Object> resultMap = new HashMap<String, Object>();
			resultMap.put("dtPlan", dtPlans.get(i));
			if (orderInfoList != null && orderInfoList.size() > 0) {
				ArrayList<OrderInfo> orderInfoList1 = new ArrayList<OrderInfo>();
				for (int j = 0; j < orderInfoList.size(); j++) {
					if (orderInfoList.get(j).getBatchNo() == dtPlans.get(i).getDtPlanCount()) {
						orderInfoList1.add(orderInfoList.get(j));
					}
				}
				resultMap.put("orderInfoList", orderInfoList1);
			}
			resultList.add(resultMap);
		}
		return resultList;
	}

	@Override
	public List<OrderInfo> hotData() throws Exception {
		List<OrderInfo> hotList = orderMapper.hotData();
		return hotList;
	}

	/**
	 * 封装orderInfoQuery
	 * 
	 * @param request
	 * @return
	 */
	@Override
	public OrderInfoQuery packagingOrderInfoQuery(OrderInfoQuery orderInfoQuery) {
		return orderInfoQuery;
	}

	/**
	 * 查询采集单计划数
	 */
	@Override
	public Integer getCollectinfoPlanCount(OrderInfoQuery orderInfoQuery) {
		List<Dtplan> dtPlans = dtplanMapper.getDtplanByOrderMainId(orderInfoQuery.getOrderMainId());
		return dtPlans.size();
	}

	/**
	 * 查询预警订单的id
	 * 
	 * @param callPolice
	 * @return
	 */
	@Override
	public List<OrderInfo> QueryCallPolice(Map<String, Object> callPolice) {
		return orderMapper.queryCallPolice(callPolice);
	}
	
}
