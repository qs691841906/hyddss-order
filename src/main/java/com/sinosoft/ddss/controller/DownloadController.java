
package com.sinosoft.ddss.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sinosoft.ddss.common.base.entity.ExportEntity;
import com.sinosoft.ddss.common.constant.Constant;
import com.sinosoft.ddss.common.entity.OrderInfo;
import com.sinosoft.ddss.common.entity.privates.CloseOrder;
import com.sinosoft.ddss.common.util.DateTimeUtils;
import com.sinosoft.ddss.common.util.ExportExcelForShopcar;
import com.sinosoft.ddss.common.util.JsonUtils;
import com.sinosoft.ddss.common.util.PropertiesUtils;
import com.sinosoft.ddss.jedis.JedisClient;
import com.sinosoft.ddss.service.OrderMainService;
import com.sinosoft.ddss.service.OrderService;
import com.sinosoft.ddss.utils.FtpUtil;

@RestController
public class DownloadController {
	private static Logger LOGGER = LoggerFactory.getLogger(DownloadController.class);
	@Autowired
	private OrderService orderService;
	@Autowired
	private OrderMainService orderMainService;
	@Autowired
	private JedisClient jedisClient;

	@ResponseBody
	@RequestMapping("/oauth/download/downloadFile")
	public String downloadFile(@RequestBody ExportEntity exportEntity, HttpServletRequest request, HttpServletResponse response) {
//		public String downloadFile( ExportEntity exportEntity, HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> map = new HashMap<String, Object>();
		String param = exportEntity.getFlag();
		if (StringUtils.isBlank(param)) {
			map.put("status", false);
			map.put("msg", "Flag is null");
			return JsonUtils.objectToJson(map);
		}
		OrderInfo orderInfoById = orderService.getOrderInfoById(Long.valueOf(exportEntity.getDataIds()));
		// 1 下载文件压缩包 ， 2 下载质检报告
		if ("1".equals(param)) {
			// 文件下载路径 ftp/卫星sensor/传感器satellite/产品级别product_level/产品名称.zip
			// product_name
			String sensor = exportEntity.getSensor();
			String satellite = exportEntity.getSatellite();
			String productLevel = exportEntity.getProductLevel();
			String productName = exportEntity.getProductName();
			String ftpPath = exportEntity.getPath();
			String proDownloadAdd = orderInfoById.getProDownloadAdd();
			String productName2 = orderInfoById.getProductName();
			// sensor/satellite/productLevel/productName.zip
			// String ftpPath =
			// "/"+sensor+"/"+satellite+"/"+productLevel+"/"+productName+".zip";
			System.err.println(ftpPath);
			String[] split = ftpPath.split("/");
			productName = split[split.length - 1];
			String ftpHost = jedisClient.get("ftpIp");
			String ftpUserName = jedisClient.get("ftpName");
			String ftpPassword = jedisClient.get("ftpPwd");
			boolean boo = FtpUtil.downloadFtpFile(request, response, proDownloadAdd, productName2, ftpHost, ftpUserName,
					ftpPassword);
		} else {
			// 获取到文件地址
			String reportPath = exportEntity.getPath();
			String proDownloadAdd = orderInfoById.getProDownloadAdd();
			// reportPath = "/HY-1C/PMS/level0/ant.zip";
			String ftpHost = jedisClient.get("ftpIp");
			String ftpUserName = jedisClient.get("ftpName");
			String ftpPassword = jedisClient.get("ftpPwd");
			// String productName = "ant.zip";
			String[] split = proDownloadAdd.split("/");
			String productName = split[split.length - 1];
			boolean boo = FtpUtil.downloadFtpFile(request, response, proDownloadAdd, productName, ftpHost, ftpUserName,
					ftpPassword);
		}
	
		return JsonUtils.objectToJson(map);
	}

	/**
	 * <pre>
	 * updateOrderStatus(主订单关闭)   
	 * 创建人：宫森      
	 * 创建时间：2018年4月10日 上午11:23:40    
	 * 修改人：宫森      
	 * 修改时间：2018年4月10日 上午11:23:40    
	 * 修改备注： 
	 * &#64;param request
	 * &#64;return
	 * </pre>
	 */

	@RequestMapping("/oauth/order/updateOrderStatus")
	public String updateOrderStatus(@RequestBody CloseOrder orderInfo) {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			// 先判断是主订单关闭还是子订单关闭
			String flag = orderInfo.getFlag();
			if (StringUtils.isBlank(flag)) {
				map.put("msg", "Flag is null");
				map.put("status", false);
				return JsonUtils.objectToJson(map);
			}
			// 只关闭 1：待处理,2：处理中
			// 失败
			// flag=1 主订单关闭 flag=2子订单关闭
			if (flag.equals("1")) {
				// 获取数据id
				String orderMainId = orderInfo.getOrderId();
				if (StringUtils.isBlank(orderMainId)) {
					map.put("msg", "OrderMainId is null");
					map.put("status", false);
					return JsonUtils.objectToJson(map);
				}
				// 失败原因
				String failReason = orderInfo.getFailReason();
				if (StringUtils.isBlank(failReason)) {
					map.put("msg", "FailReason is null");
					map.put("status", false);
					return JsonUtils.objectToJson(map);
				}
				// 订单状态
				String orderStatus = orderInfo.getOrderStatus();
				if (StringUtils.isBlank(orderStatus)) {
					map.put("msg", "OrderStatus is null");
					map.put("status", false);
					return JsonUtils.objectToJson(map);
				}
				// 修改主订单以及子订单的order_status 为失败 失败原因 质检报告不合格
				Map<String, String> params = new HashMap<String, String>();
				params.put("orderStatus", orderStatus);
				params.put("orderId", orderMainId);
				params.put("failReason", failReason);
				params.put("flag", "1");
				// TODO params.put("auditor", "");
				orderMainService.updateOrderStatus(params);
				orderService.updateOrderStatus(params);
			} else {
				// 获取数据id
				String orderId = orderInfo.getOrderId();
				if (StringUtils.isBlank(orderId)) {
					map.put("msg", "OrderId is null");
					map.put("status", false);
					return JsonUtils.objectToJson(map);
				}
				// 失败原因
				String failReason = orderInfo.getFailReason();
				if (StringUtils.isBlank(failReason)) {
					map.put("msg", "FailReason is null");
					map.put("status", false);
					return JsonUtils.objectToJson(map);
				}
				// 订单状态
				String orderStatus = orderInfo.getOrderStatus();
				if (StringUtils.isBlank(orderStatus)) {
					map.put("msg", "OrderStatus is null");
					map.put("status", false);
					return JsonUtils.objectToJson(map);
				}
				// 修改主订单以及子订单的order_status 为失败 失败原因 质检报告不合格
				Map<String, String> params = new HashMap<String, String>();
				params.put("orderStatus", orderStatus);
				params.put("orderId", orderId);
				params.put("failReason", failReason);
				params.put("flag", "2");
				orderService.updateOrderStatus(params);
			}
			map.put("status", true);
			map.put("msg", "UpdateOrderStatus is success");
			LOGGER.info("UpdateOrderStatus is success");
		} catch (Exception e) {
			e.printStackTrace();
			map.put("status", false);
			map.put("msg", "UpdateOrderStatus has exception");
			LOGGER.error("UpdateOrderStatus has exception, caused by "+e.getMessage(),Constant.SYSTEM);
		}
		return JsonUtils.objectToJson(map);
	}

	/**
	 * 
	 * 订单导出
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/oauth/order/exportOrderXls", method = RequestMethod.POST)
	public String exportOrderXls(@RequestBody ExportEntity exportEntity, HttpServletResponse response) {
		Map<String, Object> map = new HashMap<String, Object>();
		// 获取当前国际化
		String language = exportEntity.getLanguage();
		String local = PropertiesUtils.getLocale(language);
		LOGGER.info("Access exporXls & Xml");
		// 获取选择的订单id
		String dataIds = exportEntity.getDataIds();
		if (StringUtils.isBlank(dataIds)) {
			map.put("status", false);
			map.put("msg", "Data ids is null");
			return JsonUtils.objectToJson(map);
		}
		String flag = exportEntity.getFlag();// 获取标识
		// 打开输出流
		try {
			OutputStream out = response.getOutputStream();// 打开输出流
			response.setContentType("octets/stream");
			response.addHeader("Content-Disposition", "attachment;filename=" + DateTimeUtils.getNowStrTimeStemp() + ".xls");
			@SuppressWarnings("unchecked")
			Map<String, Object> condMap = new HashedMap();
			condMap.put("sonOrderIds", dataIds);
			List<Map<String, Object>> orderList;
			condMap.put("flag", flag);
			//先查出主订单 在查出来子订单
			orderList = orderMainService.findOrderByCond(condMap);
			// 封装数据导出
			ExportExcelForShopcar.exporstOrder("订单信息", orderList, out, DateTimeUtils.YMDHMS, local);
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOGGER.error("Export has exception, caused by "+e.getMessage(),Constant.SYSTEM);
		}
		return "";
	}
}
