//package com.sinosoft.ddss.test;
//
//import java.util.List;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import com.sinosoft.ddss.OrderApplication;
//import com.sinosoft.ddss.common.entity.KeyValue;
//import com.sinosoft.ddss.common.entity.ShopInfo;
//import com.sinosoft.ddss.common.entity.query.OrderInfoQuery;
//import com.sinosoft.ddss.common.entity.query.ShopInfoQuery;
//import com.sinosoft.ddss.dao.OrderInfoMainMapper;
//import com.sinosoft.ddss.jedis.JedisClient;
//import com.sinosoft.ddss.service.impl.OrderMainServiceImpl;
//import com.sinosoft.ddss.service.impl.ShopCarSerciceImpl;
//import com.sinosoft.ddss.utils.FastJsonUtil;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = OrderApplication.class)
//public class OrderApplicationTests {
//	
//	private static Logger log = LoggerFactory.getLogger(OrderApplicationTests.class);
//
//	@Autowired
//	public ShopCarSerciceImpl shopCarSerciceImpl;
//	@Autowired
//	private OrderMainServiceImpl orderMainService;
//	@Autowired
//	private JedisClient jedisClient;
//	@Autowired
//	private OrderInfoMainMapper orderMainMapper;
//	
//
//	// /**
//	// * 查询
//	// */
//	// @Test
//	// public void ListShopInfo() {
//	// List<ShopInfo> listShopInfo = shopCarSerciceImpl.ListShopInfo();
//	// System.out.println("*****************"+listShopInfo.size()+"***********************");
//	// System.out.println(listShopInfo);
//	// for (ShopInfo shopInfo : listShopInfo) {
//	// log.info("********************"+shopInfo);
//	// }
//	// }
//	
////	@Test
////	public void TestSumDataSize (){
////		double todayDataSize = orderMainMapper.sumDataSizeToday("admin");
////		System.out.println(todayDataSize>1590862791.6199988);
////	}
//
//	/**
//	 * 新增
//	 */
//	@Test
//	public void insertSelective() {
//		ShopInfo shopInfo = new ShopInfo();
//		shopInfo.setDataId(112L);
//		shopInfo.setOrderType((short) 1);
//		shopInfo.setSatellite("GF-2");
//		shopInfo.setSensor("PAN");
//		shopInfo.setUserName("alimei");
//		// shopInfo.setCreateTime(DateTimeUtils.getNowStrTime());
//		shopInfo.setProductLevel("Level-2");
//		shopInfo.setCloudCoverage((short) 13);
//		// for(int a = 55; a<100; a++){
//		shopInfo.setCloudCoverage((short) 1);
//		shopCarSerciceImpl.insertSelective(shopInfo);
//		// }
//		System.out.println("添加成功");
//	}
//
//	/**
//	 * 根据用户查询购物车数量
//	 */
//	@Test
//	public void getCountByQuery() {
//		ShopInfoQuery shopInfo = new ShopInfoQuery();
//		// shopInfo.setUserName("xiaoqi");
//		Integer countByQuery = shopCarSerciceImpl.getCountByQuery(shopInfo);
//		log.info("********************" + countByQuery);
//	}
//
//	/**
//	 * 分页
//	 */
//	@Test
//	public void ListShopInfo() {
//		ShopInfoQuery shopInfo = new ShopInfoQuery();
//		shopInfo.setPageSize(5);
//		shopInfo.setPage(2);
//		shopInfo.setUserName("xiaoqi");
//		List<ShopInfo> listShopInfo = shopCarSerciceImpl.ListShopInfo(shopInfo);
//		for (ShopInfo shopInfo1 : listShopInfo) {
//			log.info("********************" + shopInfo1);
//		}
//	}
//
//	@Test
//	public void getOrderSeq() {
//		Long orderIdNew1 = orderMainService.getOrderIdNew(1);
//		Long orderIdNew2 = orderMainService.getOrderIdNew(2);
//		Long orderIdNew3 = orderMainService.getOrderIdNew(3);
//		Long orderIdNew4 = orderMainService.getOrderIdNew(4);
//		Long orderIdNew5 = orderMainService.getOrderIdNew(5);
//		log.info("********************" + orderIdNew1);
//		log.info("********************" + orderIdNew2);
//		log.info("********************" + orderIdNew3);
//		log.info("********************" + orderIdNew4);
//		log.info("********************" + orderIdNew5);
//	}
//
//	// 根据主单号查子单信息、
//	@Test
//	public void queryOrderInfoByOrderMainId() {
//		// List<OrderInfo> orderInfoByOrderMain =
//		// orderInfoMapper.getOrderInfoByOrderMainId(180327000007L);
//		// for (OrderInfo orderInfo : orderInfoByOrderMain) {
//		// log.info("********************"+orderInfo);
//		// }
//	}
//
//	// 订单列表
//	@Test
//	public void orderList() {
//		Integer countByQuery = orderMainService.getCountByQuery(new OrderInfoQuery());
//		log.info("********************" + countByQuery);
//	}
//
//	// 按照状态统计订单数量
//	@Test
//	public void statisticalOrderByStatus() {
//		// List<OrderInfoQuery> result =
//		// orderService.statisticalOrderByStatus(new OrderInfoQuery());
//	}
//
//	// substring
//	@Test
//	public void subString() {
//		String batchNos = "";
//		for (int i = 5; i < 4; i++) {
//			batchNos += " or batchNo =" + i + " ";
//		}
//		if (batchNos.length() > 0) {
//			batchNos = batchNos.substring(3);
//		}
//		log.info(batchNos);
//	}
//
//	@Test
//	public void testWorkType() {
//		// 从配置中读取单位类别
//		String workTypes = jedisClient.get("workType");
//		workTypes = workTypes.replace("\\", "");
//		workTypes = workTypes.substring(1, workTypes.length()-1);
//		// 将单位类别转换为对象
//		List<KeyValue> list = FastJsonUtil.toList(workTypes, KeyValue.class);
//		// 根据id判断当前单位类别
//		for (KeyValue keyValue : list) {
//			if(Integer.parseInt(keyValue.getId())==2){
//				log.info("********************" + keyValue.getValue());
//				break;
//			}
//		}
//	}
//}
