package com.sinosoft.ddss.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sinosoft.ddss.common.base.entity.RestfulJSON;
import com.sinosoft.ddss.common.base.entity.TotalInfo;
import com.sinosoft.ddss.common.constant.Constant;
import com.sinosoft.ddss.common.entity.Additional;
import com.sinosoft.ddss.common.entity.OrderInfo;
import com.sinosoft.ddss.common.entity.OrderInfoMain;
import com.sinosoft.ddss.common.entity.SysResource;
import com.sinosoft.ddss.common.entity.User;
import com.sinosoft.ddss.common.entity.query.OrderInfoQuery;
import com.sinosoft.ddss.common.entity.query.UserQuery;
import com.sinosoft.ddss.common.util.JsonUtils;
import com.sinosoft.ddss.dao.OrderInfoMainMapper;
import com.sinosoft.ddss.dao.UserMapper;
import com.sinosoft.ddss.jedis.JedisClient;
import com.sinosoft.ddss.service.DecryptToken;
import com.sinosoft.ddss.service.OrderMainService;

@RestController
public class OrderMainController {
	private static Logger LOGGER = LoggerFactory.getLogger(OrderMainController.class);
	@Autowired
	private OrderMainService orderMainService;
	@Autowired
	private OrderInfoMainMapper orderMainMapper;
	@Autowired
	private UserMapper userMapper;

	@Autowired
	private JedisClient jedisClient;
	
	@Autowired
	private DecryptToken decryptToken;

	/**
	 * 订单列表
	 * 
	 * @param orderInfo
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/list",method=RequestMethod.POST)
	public String orderList(@RequestBody OrderInfoQuery orderInfo) {
//		public String orderList( OrderInfoQuery orderInfo) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		if(StringUtils.isBlank(orderInfo.getToken())){
			map.put("status", false);
			map.put("msg", "token is null");
			return JsonUtils.objectToJson(map);
		}
//		如果真实姓名不为空就是按姓名查找
		String realName = orderInfo.getRealName();
		String user_name = "";
		if(realName!=null&&!realName.equals("")){
//			根据真实姓名查找用户名
			UserQuery userQuery = new UserQuery();
			userQuery.setRealName(realName);
			List<User> list_user = userMapper.listUser(userQuery);
			for(User user:list_user){
				user_name +="'"+user.getUserName()+"',";
			}
			user_name = user_name.substring(0, user_name.length()-1);
		}
		
		// 查询主订单列表
		List<Additional> listOrder = null;
		// 封装分页数据
		TotalInfo totalInfo = null;
		try {
			// 产品级别
			if (!StringUtils.isBlank(orderInfo.getProductLevel())) {
				orderInfo.setOutProductLevel(orderInfo.getProductLevel());
			}
			// 解析token获取登录用户信息
			User user = decryptToken.decyptToken(orderInfo.getToken());
			if(user==null){
				map.put("status", false);
				map.put("msg", "Token already invalid, Please login");
				return JsonUtils.objectToJson(map);
			}
			
//			orderMainService.updateOrderInfoMainStatus();
			
			List<SysResource> sysSource = user.getSysSource();
			// 是否有查看全部订单的权限
			boolean allOfOrder = false;
			// 是否有查看预警订单的权限
			boolean OrderWarning = false;
			for (SysResource sysResource : sysSource) {
				if(sysResource.getEnname().equals("AllOfOrder")){
					allOfOrder = true;
				}
				if(sysResource.getEnname().equals("OrderWarning")){
					OrderWarning = true;
				}
			}
			// 如果有查询所有订单的权限，则查询所有订单
			if(allOfOrder){
				if(!user_name.equals("")){
					orderInfo.setUserName(user_name);
				}else{
					orderInfo.setUserName("");
				}
			} else{
				orderInfo.setUserName("'"+user.getUserName()+"'");
			}
			// 如果查询条件里有预警的话，将预警时间传入查询条件
			// 1：预警、2：非预警
			if(OrderWarning && (orderInfo.getOrderWarning()==1||orderInfo.getOrderWarning()==2)){
				// 订单预警时间
				orderInfo.setOrderWaringTime(Integer.parseInt(jedisClient.get("orderProcessingTime")));
				listOrder = this.orderMainService.ListOrder(orderInfo);
				// 查询预警订单，将所有订单设为预警
				if(orderInfo.getOrderWarning()==1){
					for (Additional orderInfo1 : listOrder) {
						orderInfo1.setCallPolice(true);
					}
				}
			}else{
				// 订单信息
				listOrder = this.orderMainService.ListOrder(orderInfo);
				String orderMainIds = "";
				if(listOrder.size()>0){
					StringBuilder orderMainIdB = new StringBuilder();
					for (Additional order : listOrder) {
						orderMainIdB.append(order.getOrderMainId()+",");
					}
					// 用户当前页的所有订单查询是需要预警
					orderMainIds = orderMainIdB.toString().substring(0, orderMainIdB.length()-1);
					// 订单预警
					if(OrderWarning && !StringUtils.isBlank(orderMainIds)){
						orderMainIds = " order_main_id in ("+orderMainIds+")";
						Map<String, Object> callPolice = new HashMap<>();
						// 订单预警时间
						String orderWaringTime = jedisClient.get("orderProcessingTime");
						callPolice.put("orderWaringTime", Integer.parseInt(orderWaringTime));
						callPolice.put("orderMainIds", orderMainIds);
						// 查询出当前页需要预警的订单
						List<Additional> listOrderCallPolice = this.orderMainService.QueryCallPolice(callPolice);
						for (Additional additional : listOrderCallPolice) {
							for (Additional additional2 : listOrder) {
								if(additional2.getOrderMainId().equals(additional.getOrderMainId())){
									additional2.setCallPolice(true);
									break;
								}
							}
						}
					}
				}
			}
			// 查询订单数量
			Integer countOrderInfo = this.orderMainService.getCountByQuery(orderInfo);
			// 订单统计是统计所有订单，不只是当前状态的订单
			orderInfo.setOrderMainStatus(null);
			orderInfo.setOrderType(null);
			// 根据订单状态的统计
			List<OrderInfoQuery> orderCount = this.orderMainService.statisticalOrderMainByStatus(orderInfo);
			totalInfo = new TotalInfo(countOrderInfo, orderInfo.getPageSize(), orderInfo.getPage(),
					orderInfo.getStartNum());
			// 返回数据集合
			map.put("data", listOrder);
			// 分页信息
			map.put("totalInfo", totalInfo);
			// 订单数量
			map.put("orderCount", orderCount);
			// 接口状态
			map.put("status", "success");
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			LOGGER.error("Order List Query has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			map.put("status", false);
			map.put("msg", "error");
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 采集单订单列表
	 * 
	 * @param orderInfo
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/collectinfoList", method = RequestMethod.POST)
	public String collectinfoList(@RequestBody OrderInfoQuery orderInfo) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		// 查询主订单列表
		List<Additional> listOrder = null;
		// 封装分页数据
		TotalInfo totalInfo = null;
		try {
			// 解析token获取登录用户信息
			User user = decryptToken.decyptToken(orderInfo.getToken());
			List<SysResource> sysSource = user.getSysSource();
			// 是否有查看全部订单的权限
			boolean allOfOrder = false;
			// 是否有查看预警订单的权限
			boolean OrderWarning = false;
			for (SysResource sysResource : sysSource) {
				if(sysResource.getEnname().equals("AllOfCollectOrder")){
					allOfOrder = true;
				}
				if(sysResource.getEnname().equals("CollectOrderWarning")){
					OrderWarning = true;
				}
			}
			if(allOfOrder){
				orderInfo.setUserName("");
			} else{
				orderInfo.setUserName(user.getUserName());
			}
			if (orderInfo.getOrderType() == null)
				orderInfo.setOrderType((short) 8);
			listOrder = this.orderMainService.ListOrder(orderInfo);
			String orderMainIds = "";
			if(listOrder.size()>0){
				StringBuilder orderMainIdB = new StringBuilder();
				for (Additional order : listOrder) {
					orderMainIdB.append(order.getOrderMainId()+",");
				}
				orderMainIds = orderMainIdB.toString().substring(0, orderMainIdB.length()-1);
				orderMainIds = " order_main_id in ("+orderMainIds+")";
				// 订单预警
				if(OrderWarning){
					Map<String, Object> callPolice = new HashMap<>();
					// 采集单审核预警时间
					String orderWaringTime = jedisClient.get("collectionReviewTime");
					callPolice.put("orderWaringTime", Integer.parseInt(orderWaringTime));
					callPolice.put("orderMainIds", orderMainIds);
					
					List<Additional> listOrderCallPolice = this.orderMainService.QueryCallPolice(callPolice);
					
					for (int i = 0; i < listOrder.size(); i++) {
						Additional additional = listOrder.get(i);
						for (int j = 0; j < listOrderCallPolice.size(); j++) {
							if (additional.getOrderMainId().equals(listOrderCallPolice.get(j).getOrderMainId())) {
								additional.setCallPolice(true);
								break;
							}
						}
						listOrder.set(i, additional);
					}
				}
			}
			// 查询订单数量
			Integer countOrderInfo = this.orderMainService.getCountByQuery(orderInfo);
			totalInfo = new TotalInfo(countOrderInfo, orderInfo.getPageSize(), orderInfo.getPage(),
					orderInfo.getStartNum());
			// 根据订单状态的统计
			List<OrderInfoQuery> orderCount = this.orderMainService.statisticalCollectinfoByStatus(orderInfo);
			totalInfo = new TotalInfo(countOrderInfo, orderInfo.getPageSize(), orderInfo.getPage(),
					orderInfo.getStartNum());
			map.put("orderCount", orderCount);
			// 返回数据集合
			map.put("data", listOrder);
			// 分页信息
			map.put("totalInfo", totalInfo);
			// 接口状态
			map.put("status", true);
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			LOGGER.error("Collectinfo List Query has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 订单审核
	 * 
	 * @param orderInfoMain
	 * @param request
	 *            orderMainIds
	 * @return
	 */
//	@RequestMapping(value = "/oauth/order/audit")
	@RequestMapping(value = "/oauth/order/audit", method = RequestMethod.POST)
	public String auditOrder(@RequestBody OrderInfoQuery orderInfoQuery) {
//	public String auditOrder( OrderInfoQuery orderInfoQuery) {
//		public String auditOrder( OrderInfoQuery orderInfoQuery) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		// token获取当前用户设置审核人
		if(StringUtils.isBlank(orderInfoQuery.getToken())){
			map.put("msg", "token is null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		User user = decryptToken.decyptToken(orderInfoQuery.getToken());
		// token获取当前用户设置审核人
		if(user==null){
			map.put("msg", "Token has expired");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		orderInfoQuery.setAuditor(user.getUserName());
		// 获取主单id
		String orderMainIds = orderInfoQuery.getOrderMainIds();
		if (StringUtils.isBlank(orderMainIds)) {
			map.put("msg", "orderMainIds null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		int priority = orderInfoQuery.getPriority();
		LOGGER.info("priority-------------------------"+priority);
		
		// 失败原因
		String auditFailReason = orderInfoQuery.getAuditFailReason();
		// 获取审核状态
		Short auditStatus = orderInfoQuery.getAuditStatus();
		if (auditStatus == null) {
			map.put("msg", "auditStatus null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		} else {
			if (auditStatus == 3 && auditFailReason == null) {
				map.put("msg", "auditStatus null");
				map.put("status", false);
				return JsonUtils.objectToJson(map);
			}
		}
		try {
			boolean result = this.orderMainService.auditOrder(orderInfoQuery);
			if (result) {
				map.put("status", true);
				return JsonUtils.objectToJson(map);
			} else {
				map.put("msg", "audit false");
				map.put("status", false);
				return JsonUtils.objectToJson(map);
			}
		} catch (Exception e) {
			LOGGER.error("Order audit has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			map.put("msg", "error");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 订单取消恢复
	 * 
	 * @param orderInfoMain
	 * @param request
	 *            orderMainId 主订单号 cancelStatus 取消恢复
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/cancel", method = RequestMethod.POST)
	public String cancelOrder(@RequestBody OrderInfoMain orderInfoMain) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			Map<String, Object> result = this.orderMainService.cancelOrder(orderInfoMain);
			if ((boolean) result.get("status")) {
				map.put("status", true);
				return JsonUtils.objectToJson(map);
			} else {
				map.put("msg", result.get("msg"));
				map.put("status", false);
				return JsonUtils.objectToJson(map);
			}
		} catch (Exception e) {
			LOGGER.error("Order cancel has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 订单删除
	 * 
	 * @param orderInfoMain
	 * @param request
	 *            orderMainId 主订单号
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/delOrder", method = RequestMethod.POST)
	public String delOrder(String orderMainId) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		// 获取id名称
		// String orderMainId = request.getParameter("orderMainId");
		if (StringUtils.isBlank(orderMainId)) {
			map.put("msg", "orderMainId null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		try {
//			可以删除的订单号（订单号，订单号）
			String orderMainIds = "";
//			不可删除的订单号（订单号，订单号）
			String notorderMainIds = "";
			
			List<OrderInfoMain> orderMainList = orderMainMapper.listOrderByOrderMainIds(orderMainId);
			for(OrderInfoMain orderinfomain:orderMainList){
				int ordermainstatus = orderinfomain.getOrderMainStatus();
				String id = String.valueOf(orderinfomain.getOrderMainId());
//				订单状态是完成、取消、失败的可以删除其他不可删除
				if(ordermainstatus==5||ordermainstatus==6||ordermainstatus==7){
					orderMainIds+=id+",";
				}else{
					notorderMainIds+=id+",";
				}
				
			}
			
			Integer result = 0;
			
			if(orderMainIds.length()>0){
				
				orderMainIds = orderMainIds.substring(0,orderMainIds.length() - 1);
				result = this.orderMainService.delOrder(orderMainIds);
			}
			if(notorderMainIds.length()>0){
				
				notorderMainIds = notorderMainIds.substring(0,notorderMainIds.length() - 1);
			}
			
			if (result > 0) {
				map.put("status", true);
				map.put("msg", "success");
				map.put("notorderMainIds", notorderMainIds);
				return JsonUtils.objectToJson(map);
			} else {
				map.put("msg", "update failure");
				map.put("status", false);
				map.put("notorderMainIds", notorderMainIds);
				return JsonUtils.objectToJson(map);
			}
		} catch (Exception e) {
			LOGGER.error("Delete Order has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			map.put("msg", "error");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 根据orderMainId查询订单
	 * 
	 * @param orderInfoMain
	 * @param request
	 *            orderMainId 主订单号
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/getOrderMainById", method = RequestMethod.POST)
	public String getOrderMainById(String orderMainId) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		// 获取id名称
		// String orderMainId = request.getParameter("orderMainId");
		if (StringUtils.isBlank(orderMainId)) {
			map.put("msg", "orderMainId null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		try {
			Additional orderInfoMain = this.orderMainService.getOrderMainById(Long.parseLong(orderMainId));
			map.put("data", orderInfoMain);
			map.put("status", true);
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			LOGGER.error("Get Order Main By Id Order has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			map.put("msg", "error");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 修改主单信息
	 * 
	 * @param orderInfoMain
	 * @param request
	 *            orderMainId 主订单号
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/updateOrderInfoMianById", method = RequestMethod.POST)
	public String updateOrderInfoMianById(@RequestBody Additional orderInfoMain) {
		LOGGER.error(orderInfoMain.toString());
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		// 获取id名称
		Long orderMainId = orderInfoMain.getOrderMainId();
		if (orderMainId == null) {
			map.put("msg", "orderMainId null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		try {
			boolean result = this.orderMainService.updateOderMainInfo(orderInfoMain);
			if (result) {
				map.put("status", true);
				return JsonUtils.objectToJson(map);
			} else {
				map.put("status", false);
				map.put("msg", "update false");
				return JsonUtils.objectToJson(map);
			}
		} catch (Exception e) {
			LOGGER.error("Update Order Main By Id Order has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			map.put("msg", "error");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
	}
	/**
	 * 根据ordermain id 获取 采集单信息
	 * @param userName
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/selectOrderMainById")
	public String selectOrderMainById(String orderMainId){
		try {
			Additional orderInfoMain = this.orderMainService.getOrderMainById(Long.parseLong(orderMainId));
			return JsonUtils.objectToJson(orderInfoMain);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 普通用户工作台订单统计，展示订单处理状态信息
	 * @param orderType 订单类型 0：订单、8：采集单
	 * 
	 * @return
	 */
	@RequestMapping(value = "/oauth/deskOrderStatistics",method=RequestMethod.POST)
	public String deskOrderStatistics(@RequestBody OrderInfoQuery orderInfo){
		RestfulJSON restJson = new RestfulJSON();
		restJson.setStatus(false);
		if(orderInfo.getOrderType() != 0 && orderInfo.getOrderType() != 8){
			restJson.setMsg("OrderType param is error");
			return JsonUtils.objectToJson(restJson);
		}
		String token = orderInfo.getToken();
		if(StringUtils.isNoneBlank(token)){
			// 解析token获取登录用户信息
			User user = decryptToken.decyptToken(orderInfo.getToken());
			orderInfo.setUserName(user.getUserName());
		}else{
			restJson.setMsg("token is error");
			return JsonUtils.objectToJson(restJson);
		}
		try {
			// 根据订单状态的统计
			List<OrderInfoQuery> orderCount = this.orderMainService.statisticalOrderMainByStatus(orderInfo);
			restJson.setStatus(true);
			restJson.setData(orderCount);
		} catch (Exception e) {
			restJson.setMsg("error "+ e.getMessage());
			e.printStackTrace();
		}
		return JsonUtils.objectToJson(restJson);
	}
}
