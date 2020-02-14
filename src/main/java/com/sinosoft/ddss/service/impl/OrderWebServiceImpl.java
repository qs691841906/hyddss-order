package com.sinosoft.ddss.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.jws.WebService;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.sinosoft.ddss.common.base.entity.TotalInfo;
import com.sinosoft.ddss.common.constant.Constant;
import com.sinosoft.ddss.common.entity.Additional;
import com.sinosoft.ddss.common.entity.Metadata;
import com.sinosoft.ddss.common.entity.OrderInfo;
import com.sinosoft.ddss.common.entity.ShopInfo;
import com.sinosoft.ddss.common.entity.User;
import com.sinosoft.ddss.common.entity.query.OrderInfoQuery;
import com.sinosoft.ddss.common.entity.query.ShopInfoQuery;
import com.sinosoft.ddss.dao.OrderInfoMainMapper;
import com.sinosoft.ddss.dao.OrderInfoMapper;
import com.sinosoft.ddss.dao.ShopInfoMapper;
import com.sinosoft.ddss.dao.UserMapper;
import com.sinosoft.ddss.dataDao.ddssMetadataMapper;
import com.sinosoft.ddss.jedis.JedisClient;
import com.sinosoft.ddss.service.DecryptToken;
import com.sinosoft.ddss.service.OrderMainService;
import com.sinosoft.ddss.service.OrderWebService;
import com.sinosoft.ddss.service.ShopCarService;
import com.sinosoft.ddss.utils.ReturnSSPXmlUtils;
import com.sinosoft.ddss.utils.ReturnXMLUtil;
import com.sinosoft.ddss.utils.ValidatorXML;
import com.sinosoft.ddss.utils.XmlToMapUtils;

@WebService(serviceName = "orderWebService", // 与接口中指定的name一致
		targetNamespace = "http://service.ddss.sinosoft.com/", // 与接口中的命名空间一致,一般是接口的包名倒
		endpointInterface = "com.sinosoft.ddss.service.OrderWebService"// 接口地址
)
@Service
@Transactional
public class OrderWebServiceImpl implements OrderWebService {

	private static Logger LOGGER = LoggerFactory.getLogger(OrderWebServiceImpl.class);
	@Autowired
	private OrderInfoMapper orderInfoMapper;
	@Autowired
	private OrderMainService orderMainService;
	@Autowired
	private OrderInfoMainMapper orderMainMapper;
	@Autowired
	private JedisClient jedisClient;
	@Autowired
	private ddssMetadataMapper metadataQueryMapper;
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private ShopCarService shopCarService;
	@Autowired
	private ShopInfoMapper shopInfoMapper;
	@Autowired
	private DecryptToken decryptToken;
	@Autowired
	private UserMapper userMapper;

	/**
	 * 与共享：提交订单
	 */
	@Override
	public String SSP_DDSS_ORDERSUBMIT(String xmlInfo) {
		LOGGER.info("*********提交订单接口**********");
		System.out.println(xmlInfo);
		// 获取Xml头部信息
		Map<String, Object> headMap = XmlToMapUtils.getHeadMap(xmlInfo);
		//接口实体类型
		String messageType = (String) headMap.get(Constant.MESSAGE_TYPE);
		//消息标识号，由接口服务调用方负责填写，递增到上限后可回滚
		String messageID = (String) headMap.get(Constant.MESSAGE_ID);
		//发送方
		String originatorAddress = (String) headMap.get(Constant.ORIGINATOR_ADDRESS);
		//接收方
		String recipientAddress = (String) headMap.get(Constant.RECIPIENT_ADDRESS);
		
//		String result=ValidatorXML.validatorXml(xmlInfo, "SSP/SSP_DDSS_ORDERSUBMIT");
		
//		if(result.contains(Constant.REPLYINFO_FAIL)){
//			return ReturnXMLUtil.xmlResult(false, messageType, messageID, originatorAddress, recipientAddress, result);
//		}
		
		// 获取参数
		Map<String, Object> paramMap = XmlToMapUtils.getParamMap(xmlInfo);
		// 获取订单参数
		// 订单名称
		String orderName = (String) paramMap.get("orderName");
		if (StringUtils.isBlank(orderName)) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
					"orderName is null");
		}
		// 分发方式（下载代表外网，离线代表内网）
		String orderMediumId = (String) paramMap.get("orderMediumId");
		if (StringUtils.isBlank(orderMediumId)) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
					"orderMediumId is null");
		}
		// 用户名
		String userName = (String) paramMap.get("userName");
		if (StringUtils.isBlank(userName)) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
					"userName is null");
		}
		// 下单时间
		String creatTime = (String) paramMap.get("creatTime");
		if (StringUtils.isBlank(creatTime)) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
					"creatTime is null");
		}
		// 购物车订单
		/*String shopinfoID = (String) paramMap.get("shopinfoID");
		if (StringUtils.isBlank(shopinfoID)) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
					"shopinfoID is null");
		}*/
		// 数管的DATAID定义
		String DMSSDataID = (String) paramMap.get("DMSSDataID");
		if (StringUtils.isBlank(DMSSDataID)) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
					"DMSSDataID is null");
		}
		// 判断DATAID是否存在
		String dataIds = "";
		int count = 0;
		if(DMSSDataID.indexOf("|") != (-1)){
			dataIds = DMSSDataID.replace("|", ",");
			count = dataIds.split(",").length;
		} else {
			dataIds = DMSSDataID;
			count = 1;
		}
		dataIds = " data_id in ("+dataIds+")";
		try {
			// 查询元数据信息
			List<Metadata> listDatas = metadataQueryMapper.listDatasByOr(dataIds);
			// 元数据信息与参数不匹配
			if (listDatas.size() != count) {
				return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
						"DMSSDataID mismatch");
			}
			// 获取订单审核方式
			String orderAutomaticReview = jedisClient.get("orderAutomaticReview");
			// 主单信息
			Additional orderInfoMain = new Additional();
			// 订单名称
			orderInfoMain.setOrderName(orderName);
			// 订单类型（1：普通订购）
			orderInfoMain.setOrderMainStatus((short) 1);
//			订单类型
			orderInfoMain.setOrderType((short)1);
			// 子单个数
			orderInfoMain.setRealOrderCount(listDatas.size());
			// 分发方式（1：在线，2：离线）
			orderInfoMain.setDistributionType(orderMediumId.equals("DOWNLOAD") ? (short) 1 : (short) 2);
			// 分发状态
			orderInfoMain.setDistributionStatus((short) 0);
			// 是否删除
			orderInfoMain.setIsDel((short) 1);
			// 取消恢复状态（0：取消状态,1：恢复状态）
			orderInfoMain.setCancelStatus((short) 1);
			// 审核状态 （1：待审核 ,2：审核通过 ,3：审核失败）
			orderInfoMain.setAuditStatus((short) (orderAutomaticReview.equals("1") ? 2 : 1));
			// 订单状态初始为待处理（1：待处理 ，2：处理中 ，3：规划中，4：采集中的5:完成 ，6：取消，7：关闭，8：失败）
			orderInfoMain.setOrderMainStatus((short) 1);
			// 用户名
			orderInfoMain.setUserName(userName);
			User user0 = new User();
			user0.setUserName(userName);
			User user = userMapper.getUserByName(user0);
			// 工作单位
			orderInfoMain.setUserUnit(user.getWorkUnit());
			// 行业类别
			orderInfoMain.setWorkType(user.getWorkType());
			// 获取主单id
			Long orderMainId = orderMainService.getOrderIdNew(1);
			orderInfoMain.setOrderMainId(orderMainId);
			// 优先级 TODO
			// 生成主单
			orderMainMapper.saveOrderMain(orderInfoMain);
			List<OrderInfo> orderList = new ArrayList<OrderInfo>();
			for (Metadata metadata : listDatas) {
				// 定义子单信息
				OrderInfo orderInfo = new OrderInfo();
				// 获取子单id
				Long orderInfoId = orderMainService.getOrderIdNew(8);
				orderInfo.setOrderId(orderInfoId);
				// 定义主单id
				orderInfo.setOrderMainId(orderMainId);
				// TODO 优先级
				// 数据id
				orderInfo.setDataId(metadata.getDataId());
				orderInfo.setOutProductLevel(metadata.getProductLevel());
				// 定义任务类型(1:订购)
				orderInfo.setOrderType((short) 1);
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
				orderInfo.setUserName(userName);
				// 用户单位
				orderInfo.setUserUnit(user.getWorkUnit());
				// 单位类别
				orderInfo.setWorkType(user.getWorkType());
//				订单类型
				orderInfo.setOrderType((short)1);
				
				// 产品下载地址
				orderInfo.setProDownloadAdd("/" + satellite + "/" + sensor + "/" + metadata.getProductLevel() + "/");
				orderList.add(orderInfo);
			}
			// 批量添加子订单
			orderInfoMapper.saveOrder(orderList);
			return ReturnXMLUtil.xmlResult(true, messageType, messageID, recipientAddress, originatorAddress, "");
		} catch (Exception e) {
			e.printStackTrace();
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress, e.getMessage());
		}
	}

	/**
	 * 与共享：订单集信息检索与反馈
	 * 
	 * @param xmlInfo
	 * @return
	 */
	@Override
	public String SSP_DDSS_ORDERSETRETRIEVAL(String xmlInfo) {
		LOGGER.info("*********订单集信息检索与反馈接口**********");
		System.out.println(xmlInfo);
		// 获取Xml头部信息
		Map<String, Object> headMap = XmlToMapUtils.getHeadMap(xmlInfo);
		//接口实体类型
		String messageType = (String) headMap.get(Constant.MESSAGE_TYPE);
		//消息标识号，由接口服务调用方负责填写，递增到上限后可回滚
		String messageID = (String) headMap.get(Constant.MESSAGE_ID);
		//发送方
		String originatorAddress = (String) headMap.get(Constant.ORIGINATOR_ADDRESS);
		//接收方
		String recipientAddress = (String) headMap.get(Constant.RECIPIENT_ADDRESS);
		
		String result=ValidatorXML.validatorXml(xmlInfo, "SSP/SSP_DDSS_ORDERSETRETRIEVAL");
		
		if(result.contains(Constant.REPLYINFO_FAIL)){
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, originatorAddress, recipientAddress, result);
		}
		// 获取参数
		Map<String, Object> paramMap = XmlToMapUtils.getParamMap(xmlInfo);
		// 登录用户名
		String userName = (String) paramMap.get("userName");
		if (StringUtils.isBlank(userName)) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
					"userName is null");
		}
		OrderInfoQuery orderInfoQuery = new OrderInfoQuery();
		orderInfoQuery.setUserName(userName);
		// 当前页
		String currentPageStr = (String) paramMap.get("currentPage");
		if(StringUtils.isNotBlank(currentPageStr)){
			
		}
		// 每页记录数
		String pageSizeStr = (String) paramMap.get("pageSize");
		Integer currentPage = Integer.valueOf(currentPageStr);
		Integer pageSize = Integer.valueOf(pageSizeStr);
		if (currentPage != null && currentPage > 0) {
			orderInfoQuery.setPage(currentPage);
		}
		if (pageSize != null && pageSize > 0) {
			orderInfoQuery.setPageSize(pageSize);
		}
		try {
			// 查询主单
			List<Additional> listOrder = orderMainService.ListOrder(orderInfoQuery);
			List<Additional> newOrderMainList = new ArrayList<Additional>();
			// 查询每个主单下的子单信息
			for (Additional additional : listOrder) {
				Long orderMainId = additional.getOrderMainId();
				OrderInfoQuery orderInfoQuery1 = new OrderInfoQuery();
				orderInfoQuery1.setOrderMainId(orderMainId);
				List<OrderInfo> orderList = orderInfoMapper.getOrderInfoByOrderMainId(orderInfoQuery1);
				additional.setOrderList(orderList);
				newOrderMainList.add(additional);
			}
			// 查询订单数量
			Integer orderCount = orderMainService.getCountByQuery(orderInfoQuery);
			TotalInfo totalInfo = new TotalInfo(orderCount, orderInfoQuery.getPageSize(), orderInfoQuery.getPage(),
					orderInfoQuery.getStartNum());
			return ReturnSSPXmlUtils.SSP_DDSS_ORDERINFO(messageType, messageID, recipientAddress, originatorAddress,
					newOrderMainList, totalInfo);
		} catch (Exception e) {
			e.printStackTrace();
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, originatorAddress, recipientAddress, e.getMessage());
		}
	}

	/**
	 * 共享：加入购物车
	 */
	@Override
	public String SSP_DDSS_ADDSHOPCAR(String xmlInfo) {
		// 获取Xml头部信息
		Map<String, Object> headMap = XmlToMapUtils.getHeadMap(xmlInfo);
		//接口实体类型
		String messageType = (String) headMap.get(Constant.MESSAGE_TYPE);
		//消息标识号，由接口服务调用方负责填写，递增到上限后可回滚
		String messageID = (String) headMap.get(Constant.MESSAGE_ID);
		//发送方
		String originatorAddress = (String) headMap.get(Constant.ORIGINATOR_ADDRESS);
		//接收方
		String recipientAddress = (String) headMap.get(Constant.RECIPIENT_ADDRESS);
		
		String result=ValidatorXML.validatorXml(xmlInfo, "SSP/SSP_DDSS_ADDSHOPCAR");
		
		if(result.contains(Constant.REPLYINFO_FAIL)){
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, originatorAddress, recipientAddress, result);
		}
		// 获取参数
		Map<String, Object> paramMap = XmlToMapUtils.getParamMap(xmlInfo);
		// userName 登录用户名
		String userName = (String) paramMap.get("userName");
		if (StringUtils.isBlank(userName)) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
					"userName is null");
		}
		// DMSSDataID
		String DMSSDataID = (String) paramMap.get("DMSSDataID");
		if (StringUtils.isBlank(DMSSDataID)) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
					"DMSSDataID is null");
		}
		ShopInfo shopInfo = new ShopInfo();
		// 订购
		shopInfo.setOrderType((short) 2);
		shopInfo.setUserName(userName);
		try {
			Integer resulr = shopCarService.saveShopCar(DMSSDataID, shopInfo);
			if (resulr > 0) {
				return ReturnXMLUtil.xmlResult(true, messageType, messageID, recipientAddress, originatorAddress, "");
			} else {
				return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
						"Add shopcar error");
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage());
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress, e.getMessage());
		}
	}

	/**
	 * 与共享：购物车查询
	 * 
	 * @param xmlInfo
	 * @return
	 */
	@Override
	public String SSP_DDSS_SHOPCARRIEVAL(String xmlInfo) {
		System.out.println(xmlInfo);
		// 获取Xml头部信息
		Map<String, Object> headMap = XmlToMapUtils.getHeadMap(xmlInfo);
		//接口实体类型
		String messageType = (String) headMap.get(Constant.MESSAGE_TYPE);
		//消息标识号，由接口服务调用方负责填写，递增到上限后可回滚
		String messageID = (String) headMap.get(Constant.MESSAGE_ID);
		//发送方
		String originatorAddress = (String) headMap.get(Constant.ORIGINATOR_ADDRESS);
		//接收方
		String recipientAddress = (String) headMap.get(Constant.RECIPIENT_ADDRESS);
		
		String result=ValidatorXML.validatorXml(xmlInfo, "SSP/SSP_DDSS_SHOPCARRIEVAL");
		
		if(result.contains(Constant.REPLYINFO_FAIL)){
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, originatorAddress, recipientAddress, result);
		}
		// 获取参数
		Map<String, Object> paramMap = XmlToMapUtils.getParamMap(xmlInfo);
		// 登录用户名
		String userName = (String) paramMap.get("userName");
		if (StringUtils.isBlank(userName)) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
					"userName is null");
		}
		ShopInfoQuery shopInfoQuery = new ShopInfoQuery();
		shopInfoQuery.setUserName(userName);
		// 当前页
		String currentPageStr = (String) paramMap.get("currentPage");
		// 每页记录数
		String pageSizeStr = (String) paramMap.get("pageSize");
		Integer currentPage = Integer.valueOf(currentPageStr);
		Integer pageSize = Integer.valueOf(pageSizeStr);
		if (currentPage != null && currentPage > 0) {
			shopInfoQuery.setPage(currentPage);
		}
		if (pageSize != null && pageSize > 0) {
			shopInfoQuery.setPageSize(pageSize);
		}
		// 查询购物车列表
		List<ShopInfo> listShopInfo = this.shopCarService.ListShopInfo(shopInfoQuery);
		Integer countShopInfo = this.shopCarService.getCountByQuery(shopInfoQuery);
		TotalInfo totalInfo = new TotalInfo(countShopInfo, shopInfoQuery.getPageSize(), shopInfoQuery.getPage(),
				shopInfoQuery.getStartNum());
		return ReturnSSPXmlUtils.SSP_DDSS_SHOPCARINFO(messageType, messageID, recipientAddress, originatorAddress,
				listShopInfo, totalInfo);
	}

	/**
	 * 与共享：购物车删除
	 * 
	 * @param xmlInfo
	 * @return
	 */
	@Override
	public String SSP_DDSS_SHOPCARDELETE(String xmlInfo) {
		// 获取Xml头部信息
		System.out.println(xmlInfo);
		// 获取Xml头部信息
		Map<String, Object> headMap = XmlToMapUtils.getHeadMap(xmlInfo);
		//接口实体类型
		String messageType = (String) headMap.get(Constant.MESSAGE_TYPE);
		//消息标识号，由接口服务调用方负责填写，递增到上限后可回滚
		String messageID = (String) headMap.get(Constant.MESSAGE_ID);
		//发送方
		String originatorAddress = (String) headMap.get(Constant.ORIGINATOR_ADDRESS);
		//接收方
		String recipientAddress = (String) headMap.get(Constant.RECIPIENT_ADDRESS);
		
		String result=ValidatorXML.validatorXml(xmlInfo, "SSP/SSP_DDSS_SHOPCARDELETE");
		
		if(result.contains(Constant.REPLYINFO_FAIL)){
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, originatorAddress, recipientAddress, result);
		}
		// 获取参数
		Map<String, Object> paramMap = XmlToMapUtils.getParamMap(xmlInfo);
		// 登录用户名
		String userName = (String) paramMap.get("userName");
		if (StringUtils.isBlank(userName)) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
					"userName is null");
		}
		// 数据产品唯一标识
		String DMSSDataID = (String) paramMap.get("DMSSDataID");
		if (StringUtils.isBlank(DMSSDataID)) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,
					"DMSSDataID is null");
		}
		String dataIds = "";
		int count = 0;
		if(DMSSDataID.indexOf("|") != (-1)){
			dataIds = DMSSDataID.replace("|", ",");
			count = dataIds.split(",").length;
		} else {
			dataIds = DMSSDataID;
			count = 1;
		}
		dataIds = " data_id in ("+dataIds+")";
		// 清除购物车数据
		ShopInfo shopInfo = new ShopInfo();
		shopInfo.setUserName(userName);
		shopInfo.setDataIds(dataIds);
		try {
			int delResult = shopInfoMapper.deleteByDataIds(shopInfo);
			if (delResult > 0) {
				return ReturnXMLUtil.xmlResult(true, messageType, messageID, recipientAddress, originatorAddress, "");
			} else {
				return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress,"No data to be deleted");
			}
		} catch (Exception e) {
			return ReturnXMLUtil.xmlResult(false, messageType, messageID, recipientAddress, originatorAddress, e.getMessage());
		}
	}

	/**
	 * 与共享 用户观测需求接口
	 * 
	 * @param xmlInfo
	 * @return
	 */
	@Override
	public String DDSS_SSP_OBSTASK(String xmlInfo) {
		// TODO
		return null;
	}

	public String getDate(Date date) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		String dates = "";
		if (null != date && !"".equals(date)) {
			dates = df.format(date);
		} else {
			dates = df.format(new Date());
		}
		return dates;
	}

	/**
	 * 解析xml头部标签
	 * 
	 * @param xmlInfo
	 * @return
	 */
	private Map getHeadMap(String xmlInfo) {
		// 校验xml参数
		XmlToMapUtils mapUtil = new XmlToMapUtils();// 解析xml工具类
		Map<String, Object> map = mapUtil.Dom4jMap(xmlInfo);// 解析检索条件
		// 具体实现map
		Map headMap = (Map) map.get("FileHead");
		return headMap;
	}

	/**
	 * 手机端接口总闸
	 * @param xmlInfo
	 * @return
	 */
	@Override
	public String SSP_DDSS_MOBILE(String xmlInfo) {
		LOGGER.info("-------------------------------------收到xml------------------------------------");
		LOGGER.info(xmlInfo);
		
		// 先拿到令牌（用户和方法）
		// 获取Xml头部信息
		Map<String, Object> headMap = XmlToMapUtils.getHeadMap(xmlInfo);
		//接口实体类型
		String messageType = (String) headMap.get(Constant.MESSAGE_TYPE);
		
		String token = (String) headMap.get("token");
		if(null==token&&!"SSP_DDSS_USERLOGIN".equals(messageType)&&!"SSP_DDSS_USERREGIST".equals(messageType)&&!"SSP_DDSS_FORGOTPSW".equals(messageType)&&!"SSP_DDSS_SETNEWPSW".equals(messageType)&&!"SSP_DDSS_CONFIGINFO".equals(messageType)){
			LOGGER.info(Constant.TOKEN_NULL);
			return Constant.TOKEN_NULL;
		}
  		User user = decryptToken.decyptToken(token);
//  		如果用户不在redis中就拦截
//		登陆接口，注册接口，忘记密码接口不拦截
		if (user == null&&!"SSP_DDSS_USERLOGIN".equals(messageType)&&!"SSP_DDSS_USERREGIST".equals(messageType)&&!"SSP_DDSS_FORGOTPSW".equals(messageType)&&!"SSP_DDSS_SETNEWPSW".equals(messageType)&&!"SSP_DDSS_CONFIGINFO".equals(messageType)) {
			LOGGER.info("user is not fond");
			return "user is not fond";
		}
//		调用order服务的接口IP
		String OrderWebServiceUrl = "http://172.16.25.27:8781/services/OrderWebService?wsdl";
		String UserWebServiceUrl = "http://172.16.25.27:8178/services/UserWebService?wsdl";
		String MocsServiceUrl = "http://172.16.25.27:8996/services/MocsService?wsdl";
		String GisServiceUrl = "http://172.16.25.27:8081/services/GisService?wsdl";
		
		String url = "";
  		switch (messageType) {
//  		order服务
		case "SSP_DDSS_ORDERSUBMIT":
			url = OrderWebServiceUrl;
			break;
		case "SSP_DDSS_ORDERSETRETRIEVAL":
			url = OrderWebServiceUrl;
			break;
		case "SSP_DDSS_ADDSHOPCAR":
			url = OrderWebServiceUrl;
			break;
		case "SSP_DDSS_SHOPCARRIEVAL":
			url = OrderWebServiceUrl;
			break;
		case "SSP_DDSS_SHOPCARDELETE":
			url = OrderWebServiceUrl;
			break;
//			user服务			
		case "SSP_DDSS_USERINFO":
			url = UserWebServiceUrl;
			break;
		case "SSP_DDSS_USERLOGIN":
			url = UserWebServiceUrl;
			break;
		case "SSP_DDSS_USERREGIST":
			url = UserWebServiceUrl;
			break;
		case "SSP_DDSS_CONFIGINFO":
			url = UserWebServiceUrl;
			break;
		case "SSP_DDSS_DATASETRETRIEVAL":
			url = UserWebServiceUrl;
			break;
		case "SSP_DDSS_FORGOTPSW":
			url = UserWebServiceUrl;
			break;
		case "SSP_DDSS_SETNEWPSW":
			url = UserWebServiceUrl;
			break;	
//			api服务
		case "SSP_DDSS_ORDERFINISH":
			url = MocsServiceUrl;
			break;
//			gis服务
		case "SSP_DDSS_DATARETRIEVAL":
			url = GisServiceUrl;
			break;
			
		default:
			break;
		}
  		
  		xmlInfo = xmlInfo.replace("<token>"+token+"</token>", "");
  		
  		LOGGER.info("url:"+url);
  		LOGGER.info("messageType:"+messageType);
  		Object webService = webService(url,
  				messageType, xmlInfo.toString());
  		LOGGER.info("-------------------------------------返回xml------------------------------------");
		LOGGER.info(webService.toString());
		return String.valueOf(webService);
	}
	
	
    public static Object webService(String url, String method, String param){
    	// 创建动态客户端
        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
        Client client = dcf.createClient(url);

        // 需要密码的情况需要加上用户名和密码
        // client.getOutInterceptors().add(new ClientLoginInterceptor(USER_NAME,PASS_WORD));
        Object[] objects = new Object[0];
        try {

            // invoke("方法名",参数1,参数2,参数3....);
            objects = client.invoke(method, param);
            LOGGER.info("返回数据:" + objects[0]);
        } catch (java.lang.Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            return null;
        }
    	return objects[0];
    }
	
	
}
