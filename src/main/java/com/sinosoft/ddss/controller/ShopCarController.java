package com.sinosoft.ddss.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sinosoft.ddss.common.base.entity.TotalInfo;
import com.sinosoft.ddss.common.entity.ShopInfo;
import com.sinosoft.ddss.common.entity.User;
import com.sinosoft.ddss.common.entity.query.ShopInfoQuery;
import com.sinosoft.ddss.common.util.DateTimeUtils;
import com.sinosoft.ddss.common.util.ExportExcelForShopcar;
import com.sinosoft.ddss.common.util.JsonUtils;
import com.sinosoft.ddss.common.util.PropertiesUtils;
import com.sinosoft.ddss.service.DecryptToken;
import com.sinosoft.ddss.service.ShopCarService;

@RestController
public class ShopCarController {
	private static Logger LOGGER = LoggerFactory.getLogger(ShopCarController.class);
	@Autowired
	private ShopCarService shopCarService;
	@Autowired
	private DecryptToken decryptToken;

	/**
	 * 根据条件查询所有的购物车信息
	 * 
	 * @param shopInfo
	 * @param request
	 * @return
	 */

	@RequestMapping(value = "/oauth/shopCar/list", method = RequestMethod.POST)
	public String shopCarList(@RequestBody ShopInfoQuery shopInfo) {
//		public String shopCarList( ShopInfoQuery shopInfo) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		// 查询购物车列表
		List<ShopInfo> listShopInfo = null;
		// 封装分页数据
		TotalInfo totalInfo = null;
		try {
			// 解析token设置用户名
			if(StringUtils.isBlank(shopInfo.getToken())){
				map.put("status", false);
				map.put("msg", "token is null");
				return JsonUtils.objectToJson(map);
			}
			if(!StringUtils.isBlank(shopInfo.getProductLevel())){
				shopInfo.setProductLevel("LEVE"+shopInfo.getProductLevel());
			}
			User user = decryptToken.decyptToken(shopInfo.getToken());
			if(user==null){
				map.put("status", false);
				map.put("msg", "token has expired");
				return JsonUtils.objectToJson(map);
			}
			shopInfo.setUserName(user.getUserName());
			listShopInfo = this.shopCarService.ListShopInfo(shopInfo);
			// 查询购物车数量
			Integer countShopInfo = this.shopCarService.getCountByQuery(shopInfo);
			totalInfo = new TotalInfo(countShopInfo, shopInfo.getPageSize(), shopInfo.getPage(),
					shopInfo.getStartNum());
			// 返回数据集合
			map.put("data", listShopInfo);
			// 分页信息
			map.put("totalInfo", totalInfo);
			// 接口状态
			map.put("status", true);
			System.out.println(JsonUtils.objectToJson(map));
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			map.put("status", false);
			map.put("msg", "error");
			return JsonUtils.objectToJson(map);
		}

	}

	/**
	 * 根据id删除购物车信息
	 * 
	 * @param request
	 * @param shopInfoIds
	 * @return
	 */

	@RequestMapping(value = "/oauth/shopCar/delShopInfo", method = RequestMethod.POST)
	public String delShopInfo(@RequestBody ShopInfoQuery shopInfoQuery) {

		Map<String, Object> map = new HashMap<String, Object>();
		// 判断ids是否为空
		String shopCarIds = shopInfoQuery.getShopCarIds();
		if (StringUtils.isBlank(shopCarIds)) {
			map.put("status", false);
			map.put("msg", "shopCarIds null");
			return JsonUtils.objectToJson(map);
		}
		try {
			// 根据id删除
			int result = this.shopCarService.deleteByPrimaryKey(shopCarIds);
			map.put("status", true);
			map.put("msg", result >= shopCarIds.split(",").length ? "delete successfully" : "id not exist");
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			map.put("status", false);
			map.put("msg", "error");
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 清空购物车
	 * 
	 * @param request
	 * @return
	 */

	@RequestMapping(value = "/oauth/shopCar/delAllShopCar", method = RequestMethod.POST)
	public String delAllShopCar(String token) {
		Map<String, Object> map = new HashMap<String, Object>();
		if(StringUtils.isBlank(token)){
			map.put("status", false);
			map.put("msg", "token is null");
			return JsonUtils.objectToJson(map);
		}
		// 解析token获取登录用户信息
		User user = decryptToken.decyptToken(token);
		if(StringUtils.isBlank(user.getUserName())){
			map.put("status", false);
			map.put("msg", "user is not login");
			return JsonUtils.objectToJson(map);
		}
		try {
			int result = this.shopCarService.delAllShopCar(user.getUserName());
			map.put("status", true);
			map.put("msg", "已删除" + result + "条记录");
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			map.put("status", false);
			map.put("msg", "error");
			LOGGER.error(e.getMessage());
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * 导出exl
	 * 
	 * @param request
	 * @return
	 */

	@RequestMapping(value = "/oauth/shopCar/exportExl", method = RequestMethod.POST)
	public String exportExl(String dataIds, HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> map = new HashMap<String, Object>();
		// 获取要导出订单的id
		if (dataIds == null || dataIds.equals("")) {
			map.put("status", false);
			map.put("msg", "dataIds null");
			return JsonUtils.objectToJson(map);
		}
		OutputStream out = null;
		// 查询数据
		List<ShopInfo> listShopInfo = this.shopCarService.selectByShopCarIds(dataIds);
		if (listShopInfo == null || listShopInfo.size() <= 0) {
			map.put("status", true);
			map.put("msg", "null data");
			return JsonUtils.objectToJson(map);
		}
		List<Map<String, String>> orderList = new ArrayList<Map<String, String>>();
		for (ShopInfo shopInfo : listShopInfo) {
			Map<String, String> shopMap = new HashMap<String, String>();
			shopMap.put("dataId", shopInfo.getDataId() + "");
			shopMap.put("orderType", shopInfo.getOrderType() == 1 ? "定制" : "订购");
			shopMap.put("satellite", shopInfo.getSatellite());
			shopMap.put("sensor", shopInfo.getSensor());
			shopMap.put("createTime", shopInfo.getCreateTime());
			shopMap.put("productLevel", shopInfo.getProductLevel());
			shopMap.put("out_product_level", shopInfo.getOutProductLevel());
			shopMap.put("cloudCoverage", shopInfo.getCloudCoverage() + "");
			shopMap.put("collectionTime", shopInfo.getCollectionTime());
			shopMap.put("productionTime", shopInfo.getProductionTime());
			orderList.add(shopMap);
		}
		try {
			// 打开输出流
			out = response.getOutputStream();
			// 设置类型
			response.setContentType("octets/stream");
			//设置头部信息
			response.addHeader(
					"Content-Disposition",
					"attachment;filename="
							+ DateTimeUtils.getNowStrTimeStemp() + ".xls");
			//获取当前国际化
			String language = request.getParameter("language");
			String local = PropertiesUtils.getLocale(language);
			String head[] = { "产品号", "订单类型", "卫星", "传感器", "产品级别","输出产品级别","添加购物车时间", "云量",
					"采集时间","生产时间" };
			String body[] = { "dataId", "orderType", "satellite",
							"sensor", "productLevel", "out_product_level", "createTime",
							"cloudCoverage", "collectionTime", "productionTime" };
			//封装数据导出
			ExportExcelForShopcar.exporsOrderXls("shopcar", orderList, out,
					DateTimeUtils.YMDHMS,head,body,local);
			out.flush();
			out.close();
			map.put("status", true);
			map.put("msg", "");
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			map.put("status", false);
			map.put("msg", "error");
			System.out.println(e);
			return JsonUtils.objectToJson(map);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 加入购物车
	 * 
	 * @param request
	 * @return 数据id：dataIds，订单类型：1订购，2定制
	 */
	@RequestMapping(value = "/oauth/shopCar/saveShopCar", method = RequestMethod.POST)
	public String saveShopCar(@RequestBody ShopInfo shopInfo) {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			if(StringUtils.isBlank(shopInfo.getToken())){
				map.put("status", false);
				map.put("msg", "token null");
				return JsonUtils.objectToJson(map);
			}
			// 获取数据id
			String dataIds = shopInfo.getDataIds();
			if (dataIds == null || dataIds.equals("")) {
				map.put("status", false);
				map.put("msg", "dataIds null");
				return JsonUtils.objectToJson(map);
			}
			// 订单类型
			Short orderType = shopInfo.getOrderType();
			if (orderType == null) {
				map.put("status", false);
				map.put("msg", "orderType null");
				return JsonUtils.objectToJson(map);
			} else {
				// 定制需要输出级别
				if (orderType == 1) {
					// 产品级别
					String productLevel = shopInfo.getProductLevel();
					if (StringUtils.isBlank(productLevel)) {
						map.put("status", false);
						map.put("msg", "productLevel null");
						return JsonUtils.objectToJson(map);
					}
					shopInfo.setOutProductLevel(productLevel);
				}
			}
			// 查询购物车里是否存在此订单
			ShopInfoQuery shopInfoQuery = new ShopInfoQuery();
			StringBuilder dataIdsSB = new StringBuilder();
			String[] dataIdS = dataIds.split(",");
			for (String dataId : dataIdS) {
				dataIdsSB.append(" data_id = " + dataId + " or");
			}
			String dataIdsS = dataIdsSB.substring(0, dataIdsSB.length() - 2);
			shopInfoQuery.setDataIds(dataIdsS);
			shopInfoQuery.setOrderType(orderType);
			if(orderType == 1){
				shopInfoQuery.setOutProductLevel(shopInfo.getOutProductLevel());
			}
			User user = decryptToken.decyptToken(shopInfo.getToken());
			shopInfoQuery.setUserName(user.getUserName());
			List<ShopInfo> listShopInfo = this.shopCarService.ListShopInfo(shopInfoQuery);
			StringBuilder insertDataIds = new StringBuilder();
			StringBuilder shopDataId = new StringBuilder();
			String addDataIds = "";
			String noAddDataIds = "";
			if(listShopInfo.size()>0){
				for (String dataId : dataIdS) {
					for (ShopInfo shopInfo1 : listShopInfo) {
						if(shopInfo1.getDataId().toString().equals(dataId)){
							shopDataId.append(dataId+",");
						}
					}
					if(shopDataId.indexOf(dataId)==-1){
						insertDataIds.append(dataId+",");
					}
				}
				if(!shopDataId.toString().equals("")){
					noAddDataIds = shopDataId.substring(0,shopDataId.length()-1);
				}
				if(!insertDataIds.toString().equals("")){
					addDataIds = insertDataIds.substring(0,insertDataIds.length()-1);
				}
			}else{
				addDataIds = dataIds;
			}
			map.put("exitShopCar", noAddDataIds);
			if(!addDataIds.equals("")){
				shopInfo.setDataIds(addDataIds);
				Integer resulr = shopCarService.saveShopCar(shopInfo);
				if (resulr > 0) {
					map.put("status", true);
					return JsonUtils.objectToJson(map);
				} else {
					map.put("status", false);
					map.put("msg", "save false");
					return JsonUtils.objectToJson(map);
				}
			}
			map.put("status", true);
			return JsonUtils.objectToJson(map);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			map.put("status", false);
			map.put("msg", "error");
			return JsonUtils.objectToJson(map);
		}
	}
}
