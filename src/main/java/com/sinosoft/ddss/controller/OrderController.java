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
import org.springframework.web.client.RestTemplate;

import com.sinosoft.ddss.common.base.entity.TotalInfo;
import com.sinosoft.ddss.common.constant.Constant;
import com.sinosoft.ddss.common.entity.Additional;
import com.sinosoft.ddss.common.entity.Metadata;
import com.sinosoft.ddss.common.entity.OrderInfo;
import com.sinosoft.ddss.common.entity.OrderInfoMain;
import com.sinosoft.ddss.common.entity.ShopInfo;
import com.sinosoft.ddss.common.entity.SysResource;
import com.sinosoft.ddss.common.entity.User;
import com.sinosoft.ddss.common.entity.query.OrderInfoQuery;
import com.sinosoft.ddss.common.entity.query.ShopInfoQuery;
import com.sinosoft.ddss.common.util.JsonUtils;
import com.sinosoft.ddss.jedis.JedisClient;
import com.sinosoft.ddss.service.DecryptToken;
import com.sinosoft.ddss.service.MetaDataService;
import com.sinosoft.ddss.service.OrderMainService;
import com.sinosoft.ddss.service.OrderService;
import com.sinosoft.ddss.service.ShopCarService;

@RestController
public class OrderController {
	private static Logger LOGGER = LoggerFactory.getLogger(OrderController.class);
	@Autowired
	private OrderService orderService;
	@Autowired
	private OrderMainService orderMainService;
	@Autowired
	private ShopCarService shopCarService;
	@Autowired
	private MetaDataService metaDataService;
	@Autowired
	RestTemplate restTemplate;
	@Autowired
	private JedisClient jedisClient;
	@Autowired
	private DecryptToken decryptToken;

	/**
	 * 查询页面生成订单
	 * 
	 * @param request
	 * @return
	 */
//	public String saveOrder(@RequestBody Additional orderInfoMain) {
//	@RequestMapping(value = "/oauth/order/saveOrderByQuery")
	@RequestMapping(value = "/oauth/order/saveOrderByQuery", method = RequestMethod.POST)
	public String saveOrder(@RequestBody Additional orderInfoMain) {
//		public String saveOrder( Additional orderInfoMain) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			// 订单名称
			String orderName = orderInfoMain.getOrderName();
			if (StringUtils.isBlank(orderName)) {
				map.put("status", false);
				map.put("msg", "orderName null");
				return JsonUtils.objectToJson(map);
			}
			// 订单类型
			Short orderType = orderInfoMain.getOrderType();
			if (null == orderType) {
				map.put("status", false);
				map.put("msg", "orderType null");
				return JsonUtils.objectToJson(map);
			}
			// 分发方式
			Short distributionType = orderInfoMain.getDistributionType();
			if (null == distributionType) {
				map.put("status", false);
				map.put("msg", "distributionType null");
				return JsonUtils.objectToJson(map);
			}
			if (orderType == 1 || orderType == 3) {
				// 产品id
				String dataIds = orderInfoMain.getDataIds();
				if (StringUtils.isBlank(dataIds)) {
					map.put("status", false);
					map.put("msg", "dataIds null");
					return JsonUtils.objectToJson(map);
				}
			}
			// 专线的采集是快速订购
			if (orderType == 5 && distributionType == 3) {
				orderInfoMain.setOrderType((short) 6);
			}
			// 快速定制预计订单数
			if (orderType == 2 || orderType == 4) {
				Integer predictOrderCount = orderInfoMain.getPredictOrderCount();
				if (predictOrderCount == null) {
					map.put("status", false);
					map.put("msg", "predictOrderCount null");
					return JsonUtils.objectToJson(map);
				}
			}
			if (orderType != 1) {
				// 产品级别
				String productLevel = orderInfoMain.getOutProductLevel();
				if (orderType == 3 || orderType == 2 || orderType == 4 || orderType == 5 || orderType == 6 || orderType == 8) {
					if (StringUtils.isBlank(productLevel)) {
						map.put("status", false);
						map.put("msg", "outProductLevel null");
						return JsonUtils.objectToJson(map);
					}
				}
			}
			if (orderType == 2 || orderType == 4 || orderType == 5 || orderType == 6 || orderType == 8) {
				// 卫星
				String satellite = orderInfoMain.getSatellite();
				if (StringUtils.isBlank(satellite)) {
					map.put("status", false);
					map.put("msg", "satellite null");
					return JsonUtils.objectToJson(map);
				}
				// 传感器
				String sensor = orderInfoMain.getSensor();
				if (StringUtils.isBlank(sensor)) {
					map.put("status", false);
					map.put("msg", "sensor null");
					return JsonUtils.objectToJson(map);
				}
				// 开始时间
				String startTime = orderInfoMain.getStartTime();
				if (StringUtils.isBlank(startTime)) {
					map.put("status", false);
					map.put("msg", "startTime null");
					return JsonUtils.objectToJson(map);
				}
				// 结束时间
				String endTime = orderInfoMain.getEndTime();
				if (StringUtils.isBlank(endTime)) {
					map.put("status", false);
					map.put("msg", "endTime null");
					return JsonUtils.objectToJson(map);
				}
			}
			if (orderType == 4 || orderType == 5 || orderType == 6 || orderType == 8) {
				// AOI
				String aoi = orderInfoMain.getAoi();
				if (StringUtils.isBlank(aoi)) {
					map.put("status", false);
					map.put("msg", "aoi null");
					return JsonUtils.objectToJson(map);
				}
			}
			boolean result = orderService.saveOrder(orderInfoMain);
			if (result) {
				map.put("status", true);
				map.put("msg", "success");
				return JsonUtils.objectToJson(map);
			} else {
				map.put("status", false);
				map.put("msg", "save failed");
				return JsonUtils.objectToJson(map);
			}
		} catch (NumberFormatException e) {
			map.put("status", false);
			map.put("msg", "distributionType error");
			LOGGER.error("DistributionType has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			e.printStackTrace();
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			map.put("status", false);
			map.put("msg", "error");
			LOGGER.error("SaveOrderByQuery has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			e.printStackTrace();
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 购物车页面生成订单
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/saveOrderByShopCar", method = RequestMethod.POST)
	public String saveOrderByShopCar(@RequestBody OrderInfoMain orderInfoMain) {
//		@RequestMapping(value = "/oauth/order/saveOrderByShopCar")
//		public String saveOrderByShopCar( OrderInfoMain orderInfoMain) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			// 接受参数
			Additional additional = new Additional();
			additional.setToken(orderInfoMain.getToken());
			// 购物车数据
			String shopCarIds = orderInfoMain.getShopCarIds();
			
//			如果shopCarIds为0就将该用户的购物车都提交
			if(shopCarIds.equals("0")){
				User user = decryptToken.decyptToken(orderInfoMain.getToken());
				if (user == null) {
					map.put("msg", "token is null");
					map.put("status", false);
					return JsonUtils.objectToJson(map);
				}
				ShopInfoQuery shopInfo = new ShopInfoQuery();
				List<ShopInfo> listShopInfo = null;
				shopInfo.setUserName(user.getUserName());
				shopInfo.setPageSize(1000);
//				查询购物车列表
				listShopInfo = this.shopCarService.ListShopInfo(shopInfo);
				String shopId = "";
				for(ShopInfo shopinfo:listShopInfo){
					shopId += String.valueOf(shopinfo.getId())+",";
				}
				shopId = shopId.substring(0, shopId.length()-1);
				shopCarIds = shopId;
			}
			
			if (StringUtils.isBlank(shopCarIds)) {
				map.put("status", false);
				map.put("msg", "shopCarIds null");
				return JsonUtils.objectToJson(map);
			}
			additional.setShopCarIds(shopCarIds);
			// 订单名称
			String orderName = orderInfoMain.getOrderName();
			if (StringUtils.isBlank(orderName)) {
				map.put("status", false);
				map.put("msg", "orderName null");
				return JsonUtils.objectToJson(map);
			}
			additional.setOrderName(orderName);
			// 订单描述
			String orderMainDesc = orderInfoMain.getOrderMainDesc();
			if (!StringUtils.isBlank(orderMainDesc)) {
				additional.setOrderMainDesc(orderMainDesc);
			}
			// 分发方式
			Short distributionType = orderInfoMain.getDistributionType();
			if (distributionType == null) {
				map.put("status", false);
				map.put("msg", "distributionType null");
				return JsonUtils.objectToJson(map);
			}
			additional.setDistributionType(distributionType);
			// 保存订单信息
			boolean result = orderService.saveOrderByShopCar(additional);
			if (result) {
				map.put("status", true);
				map.put("msg", "save success");
				return JsonUtils.objectToJson(map);
			} else {
				map.put("status", false);
				map.put("msg", "save false");
				return JsonUtils.objectToJson(map);
			}
		} catch (NumberFormatException e) {
			map.put("status", false);
			map.put("msg", "distributionType error");
			LOGGER.error("SaveOrderByShopCar has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			e.printStackTrace();
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			map.put("status", false);
			map.put("msg", "save error");
			e.printStackTrace();
			LOGGER.error("SaveOrderByShopCar has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 查询页面生成订单
	 * 
	 * @param orderInfoMain
	 * @param request
	 *            orderMainId 主订单号
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/toOrderCreateByQuery", method = RequestMethod.POST)
	public String toOrderCreateQuery(@RequestBody Additional additional) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		// 获取数据id
		String dataIds = additional.getDataIds();
		if (StringUtils.isBlank(dataIds)) {
			map.put("msg", "dataIds null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		// 获取订单类型
		Short orderType = additional.getOrderType();
		if (orderType == null) {
			map.put("msg", "orderType null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		} else {
			// 定制
			if (orderType == 3 || orderType == 4 || orderType == 5 || orderType == 6) {
				// 输出产品级别
				String productLevel = additional.getOutProductLevel();
				if (StringUtils.isBlank(productLevel)) {
					map.put("msg", "productLevel null");
					map.put("status", false);
					return JsonUtils.objectToJson(map);
				}
			}
		}
		// 订单名称
		String orderName = additional.getOrderName();
		if (StringUtils.isBlank(orderName)) {
			map.put("msg", "orderMainName null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		// 订单描述
		String orderMainDesc = additional.getOrderMainDesc();
		if (StringUtils.isBlank(orderMainDesc)) {
			map.put("msg", "orderMainDesc null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		// 分发方式
		Short distributionType = additional.getDistributionType();
		if (distributionType == null) {
			map.put("msg", "distributionType null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		// AOI
		String aoi = additional.getAoi();
		if (StringUtils.isBlank(aoi)) {
			map.put("msg", "aoi null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		try {
			boolean result = this.orderService.toOrderCreate(additional);
			if (result) {
				map.put("status", true);
				return JsonUtils.objectToJson(map);
			} else {
				map.put("msg", "Jump failure");
				map.put("status", false);
				return JsonUtils.objectToJson(map);
			}
		} catch (Exception e) {
			map.put("msg", "error");
			map.put("status", false);
			LOGGER.error("To Order Create By Query has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 购物车页面跳生成订单页面
	 * 
	 * @param orderInfoMain
	 * @param request
	 *            orderMainId 主订单号
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/toOrderCreateByShopCar", method = RequestMethod.POST)
	public String toOrderCreateShopCar(String shopCarIds,String token) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
//		如果shopCarIds为0就将该用户的购物车都提交
		if(shopCarIds.equals("0")){
			User user = decryptToken.decyptToken(token);
			if (user == null) {
				map.put("msg", "token is null");
				map.put("status", false);
				return JsonUtils.objectToJson(map);
			}
			ShopInfoQuery shopInfo = new ShopInfoQuery();
			List<ShopInfo> listShopInfo = null;
			shopInfo.setUserName(user.getUserName());
			shopInfo.setPageSize(1000);
//			查询购物车列表
			listShopInfo = this.shopCarService.ListShopInfo(shopInfo);
			String shopId = "";
			for(ShopInfo shopinfo:listShopInfo){
				shopId += String.valueOf(shopinfo.getId())+",";
			}
			shopId = shopId.substring(0, shopId.length()-1);
			shopCarIds = shopId;
		}
		
		
		// 获取数据id
		if (StringUtils.isBlank(shopCarIds)) {
			map.put("msg", "shopCarIds null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		try {
			List<ShopInfo> result = this.shopCarService.selectByShopCarIds(shopCarIds);
			map.put("status", true);
			map.put("data", result);
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			LOGGER.error("To Order Create By ShopCar has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			map.put("msg", "error");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 根据主单id查询子单
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/getOrderInfoByOrderMainId", method = RequestMethod.POST)
	public String getOrderInfoByOrderMainId(@RequestBody OrderInfoQuery orderInfoQuery) {
//		public String getOrderInfoByOrderMainId( OrderInfoQuery orderInfoQuery) {
		// 主单信息
		Additional orderInfoMain = this.orderMainService.getOrderMainById(orderInfoQuery.getOrderMainId());
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		// 获取数据id
		if (orderInfoQuery.getOrderMainId() == null) {
			map.put("msg", "orderMainId null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		try {
			// 解析token获取登录用户信息
			User user = decryptToken.decyptToken(orderInfoQuery.getToken());
			// 子单集合
			List<OrderInfo> orderList = this.orderService.getOrderInfoByOrderMainId(orderInfoQuery);
			String orderMainIds = "";
			if(orderList.size()>0){
				StringBuilder orderMainIdB = new StringBuilder();
				for (OrderInfo order : orderList) {
					orderMainIdB.append(order.getOrderMainId()+",");
				}
				orderMainIds = orderMainIdB.toString().substring(0, orderMainIdB.length()-1);
				orderMainIds = " order_id in ("+orderMainIds+")";
			}
			// 是否有查看预警订单的权限
			boolean OrderWarning = false;
			if (user != null) {
				List<SysResource> sysSource = user.getSysSource();
				for (SysResource sysResource : sysSource) {
					if (sysResource.getEnname().equals("OrderWarning")) {
						OrderWarning = true;
					}
				}
			}
			if(OrderWarning && !StringUtils.isBlank(orderMainIds)){
				Map<String, Object> callPolice = new HashMap<>();
				// 订单处理预警时间
				String orderWaringTime = jedisClient.get("orderProcessingTime");
				callPolice.put("orderWaringTime", Integer.parseInt(orderWaringTime));
				callPolice.put("orderMainIds", orderMainIds);
				
				List<OrderInfo> listOrderCallPolice = this.orderService.QueryCallPolice(callPolice);
				
				for (int i = 0; i < orderList.size(); i++) {
					OrderInfo orderInfo = orderList.get(i);
					for (int j = 0; j < listOrderCallPolice.size(); j++) {
						if (orderInfo.getOrderId()==listOrderCallPolice.get(j).getOrderId()) {
							orderInfo.setCallPolice(true);
						}
					}
					if(orderInfoMain.getAuditStatus()==1){
						orderInfo.setOrderStatus((short) 1);
						orderInfoMain.setOrderMainStatus((short) 1);
					}
					String status = "";
					switch (orderInfo.getOrderStatus()) {
					case 1:
						status = "待处理";
						break;
					case 2:
						status = "处理中";
						break;
					case 3:
						status = "待分发";
						break;
					case 4:
						status = "已分发";
						break;
					case 5:
						status = "取消";
						break;
					case 6:
						status = "失败";
						break;
					case 7:
						status = "已删除";
						break;

					default:
						break;
					}
					orderInfo.setStatus(status);
					orderList.set(i, orderInfo);
				}
			}
			// 订单数量
			Integer countOrderInfo = this.orderService.getOrderCountByQuery(orderInfoQuery);
			// 分页条件
			TotalInfo totalInfo = new TotalInfo(countOrderInfo, orderInfoQuery.getPageSize(), orderInfoQuery.getPage(),
					orderInfoQuery.getStartNum());

			// 子单数量
			List<OrderInfoQuery> orderCount = this.orderService.statisticalOrderByStatus(orderInfoQuery);
			// 返回参数
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("orderInfoList", orderList);
			data.put("orderCount", orderCount);
			data.put("orderInfoMain", orderInfoMain);
			data.put("totalInfo", totalInfo);
			map.put("status", true);
			map.put("data", data);
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			LOGGER.error("Get Order Info By Order Main Id has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			map.put("msg", "error");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 子单详情
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/getOrderInfo", method = RequestMethod.POST)
	public String getOrderInfo(String orderId) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		// 获取数据id
		if (StringUtils.isBlank(orderId)) {
			map.put("msg", "orderId null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		try {
			// 子单信息
			OrderInfo orderInfo = this.orderService.getOrderInfoById(Long.parseLong(orderId));
			if(orderInfo==null){
				map.put("msg", "order is not exit");
				map.put("status", false);
				return JsonUtils.objectToJson(map);
			}
			// 元数据信息
			Metadata metadata = metaDataService.selectByPrimaryKey(orderInfo.getDataId());
			// 返回参数
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("orderInfo", orderInfo);
			data.put("metadata", metadata);
			map.put("status", true);
			map.put("data", data);
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			LOGGER.error("Get Order Info has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			map.put("msg", "error");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 根据采集单id查询子单
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/getOrderInfoByCollectionId", method = RequestMethod.POST)
	public String getOrderInfoByCollectionId(@RequestBody OrderInfoQuery orderInfoQuery) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		// 获取数据id
		Long orderMainId = orderInfoQuery.getOrderMainId();
		if (orderMainId == null) {
			map.put("msg", "orderMainId null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		try {
			// 主单信息
			Additional orderInfoMain = this.orderMainService.getOrderMainById(orderMainId);
			// 计划数
			Integer collectinfoPlanCount = this.orderService.getCollectinfoPlanCount(orderInfoQuery);
			// 分页条件
			TotalInfo totalInfo = new TotalInfo(collectinfoPlanCount, orderInfoQuery.getPageSize(),
					orderInfoQuery.getPage(), orderInfoQuery.getStartNum());
			// 子单数量
			List<OrderInfoQuery> orderCount = this.orderService.statisticalOrderByStatus(orderInfoQuery);
			// 子单信息
			List<Map<String, Object>> orderList = this.orderService.getOrderInfoByCollectionId(orderInfoQuery);
			// 返回参数
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("orderInfoList", orderList);
			data.put("orderCount", orderCount);
			data.put("orderInfoMain", orderInfoMain);
			data.put("totalInfo", totalInfo);
			map.put("status", true);
			map.put("data", data);
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			LOGGER.error("Get Order Info By Collection Id, caused by "+e.getMessage(),Constant.SYSTEM);
			map.put("msg", "error");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
	}

}
