package com.sinosoft.ddss.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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

import com.sinosoft.ddss.common.base.entity.ExportEntity;
import com.sinosoft.ddss.common.constant.Constant;
import com.sinosoft.ddss.common.entity.Metadata;
import com.sinosoft.ddss.common.util.DateTimeUtils;
import com.sinosoft.ddss.common.util.ExportExcelForShopcar;
import com.sinosoft.ddss.common.util.JsonUtils;
import com.sinosoft.ddss.common.util.PropertiesUtils;
import com.sinosoft.ddss.dataDao.ddssMetadataMapper;
import com.sinosoft.ddss.jedis.JedisClient;
import com.sinosoft.ddss.utils.XmlUtil;

@RestController
public class MetaDataController {
	private static Logger LOGGER = LoggerFactory.getLogger(ShopCarController.class);
	@Autowired
	private ddssMetadataMapper metadataQueryMapper;
	@Autowired
	private JedisClient jedisClient;
	
	/**
	 * @param request
	 * @return
	 * @author li_jiazhi
	 * @create 2018年3月26日下午4:20:26 元数据列表查询
	 */
	@RequestMapping(value="/oauth/data/list",method=RequestMethod.POST)
	public String findCityList(String dataIds) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		// 产品id
		//String dataIds = request.getParameter("dataIds");
		if (StringUtils.isBlank(dataIds)) {
			map.put("msg", "dataIds null");
			return JsonUtils.objectToJson(map);
		} else {
			List<Metadata> listDatas = metadataQueryMapper.listDatas(dataIds);
			// 返回数据集合
			map.put("data", listDatas);
			return JsonUtils.objectToJson(map);
		}
	}

	/**
	 * @param request
	 * @return
	 * @author li_jiazhi
	 * @create 2018年3月26日下午4:20:35 元数据详情查询
	 */
	@RequestMapping(value="/oauth/data/details",method=RequestMethod.POST)
	public String dataDetails(String dataId) {
		// 常见返回值对象
		Map<String, Object> map = new HashMap<String, Object>();
		// 获取id名称
//		String dataId = request.getParameter("dataId");
		if (StringUtils.isBlank(dataId)) {
			// 返回数据集合
			map.put("msg", "dataId null");
			map.put("status", false);
			return JsonUtils.objectToJson(map);
		}
		try {
			List<Metadata> metadata = metadataQueryMapper.listDatas(dataId);
			String imageIp = jedisClient.get("imageIp");
			String imagePort = jedisClient.get("imagePort");
			for (Metadata metadata2 : metadata) {
				metadata2.setThumbFileUrl(imageIp+":"+imagePort+"/"+metadata2.getThumbFileUrl());
				metadata2.setQuickFileUrl(imageIp+":"+imagePort+"/"+metadata2.getQuickFileUrl());
				String geom = String.valueOf(metadata2.getGeom());
				if(geom.indexOf("GEOMETRYCOLLECTION")>-1){
					 geom = geom.replace("GEOMETRYCOLLECTION(", "");
					 geom = geom.substring(0, geom.length()-1);
					 metadata2.setGeom(geom);
				}
			}
			// 返回数据集合
			map.put("data", metadata);
			map.put("status", true);
			return JsonUtils.objectToJson(map);
		} catch (NumberFormatException e) {
			// 返回数据集合
			map.put("msg", "error");
			map.put("status", false);
			LOGGER.error("Datails has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			return JsonUtils.objectToJson(map);
		}
	}

	@RequestMapping(value = "/public/exporXlsOrXml", method = RequestMethod.POST)
	public String dimensExporXlsproduct(@RequestBody ExportEntity exportEntity,HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> map = new HashMap<String, Object>();
		// 获取当前国际化
		String language = request.getParameter("language");
		String local = PropertiesUtils.getLocale(language);
		LOGGER.info("Access exporXls & Xml");
		String dataIds = exportEntity.getDataIds();
		if (StringUtils.isBlank(dataIds)) {
			map.put("status", false);
			map.put("msg", "DataIds is null");
			return JsonUtils.objectToJson(map);
		}
		List<Metadata> listDatas = metadataQueryMapper.listDatas(dataIds);
		if (listDatas == null || listDatas.size() <= 0) {
			map.put("status", true);
			map.put("msg", "No Data");
			return JsonUtils.objectToJson(map);
		}
		// 判断格式
		String flag = exportEntity.getFlag();
		if ("XML".equals(flag.toUpperCase())) {
			exportXml(listDatas, local, request, response);
		} else {
			exportExcel(listDatas, local, request, response);
		}
		return "";
	}

	/**
	 * <pre>
	 * exportExcel(元数据导出excel)   
	 * 创建人：宫森      
	 * 创建时间：2018年4月9日 下午4:22:27    
	 * 修改人：宫森      
	 * 修改时间：2018年4月9日 下午4:22:27    
	 * 修改备注： 
	 * &#64;param listDatas
	 * &#64;param local
	 * &#64;param request
	 * &#64;param response
	 * </pre>
	 */

	public void exportExcel(List<Metadata> listDatas, String local, HttpServletRequest request,
			HttpServletResponse response) {
		OutputStream out = null;
		try {
			List<Map<String, String>> data = new ArrayList<Map<String, String>>();
			for (Metadata md : listDatas) {
				// setting field
				Map<String, String> dataMap = new HashMap<String, String>();
				dataMap.put("data_id", object2String(md.getDataId())); // 数据id
				dataMap.put("scene_id", object2String(md.getSceneId()));// 景号
				dataMap.put("instance_id", object2String(md.getInstanceId())); // 产品号
				dataMap.put("product_name", object2String(md.getProductName())); // 产品名称
				dataMap.put("satellite", object2String(md.getSatellite())); // 卫星
				dataMap.put("sensor", object2String(md.getSensor())); // 传感器
				dataMap.put("spatial_resolution", object2String(md.getSpatialResolution())); // 产品空间分辨率
				dataMap.put("orbit_cycle", object2String(md.getOrbitCycle())); // 周期号（Cycle号）
				dataMap.put("orbit_pass", object2String(md.getOrbitPass())); // 圈号（Pass号）
				dataMap.put("orbit_id", object2String(md.getOrbitId())); // 轨道号
				dataMap.put("product_level", object2String(md.getProductLevel())); // 产品级别
				dataMap.put("product_format", object2String(md.getProductFormat())); // 产品格式
				dataMap.put("cloud_percent", object2String(md.getCloudPercent())); // 云覆盖度
				dataMap.put("product_version", object2String(md.getProductVersion())); // 数据文件版本号
				dataMap.put("produce_time", object2String(md.getProduceTime())); // 生产时间
				dataMap.put("time_zone", object2String(md.getTimeZone())); // 时区
				dataMap.put("time_type", object2String(md.getTimeType())); // 时间类型
				dataMap.put("earth_ellipsoid", object2String(md.getEarthEllipsoid())); // 坐标系
				dataMap.put("project_type", object2String(md.getProjectType())); // 投影类型
				dataMap.put("start_time", object2String(md.getImageStartTime())); // 探测开始时间
				dataMap.put("end_time", object2String(md.getImageEndTime())); // 探测结束时间
				dataMap.put("image_boundary", object2String(md.getImageBoundary())); // 数据有效范围的经纬度
				dataMap.put("top_left_latitude", object2String(md.getTopLeftLatitude())); // 左上角纬度
				dataMap.put("top_left_longitude", object2String(md.getTopLeftLongitude())); // 左上角经度
				dataMap.put("top_right_latitude", object2String(md.getTopRightLatitude())); // 右上角纬度
				dataMap.put("top_right_longitude", object2String(md.getTopRightLongitude())); // 右上角经度
				dataMap.put("bottom_right_latitude", object2String(md.getBottomRightLatitude())); // 右下角纬度
				dataMap.put("bottom_right_longitude", object2String(md.getBottomRightLongitude())); // 右下角经度
				dataMap.put("bottom_left_latitude", object2String(md.getBottomLeftLatitude())); // 左下角纬度
				dataMap.put("bottom_left_longitude", object2String(md.getBottomLeftLongitude())); // 左下角经度
				data.add(dataMap);
			}
			String[] head = { "数据id", "景号", "产品号", "产品名称", "卫星", "传感器", "产品空间分辨率", "周期号（Cycle号）", "圈号（Pass号）", "轨道号",
					"产品级别", "产品格式", "云覆盖度", "数据文件版本号", "生产时间", "时区", "时间类型", "坐标系", "投影类型", "探测开始时间", "探测结束时间",
					"数据有效范围的经纬度", "左上角纬度", "左上角经度", "右上角纬度", "右上角经度", "右下角纬度", "右下角经度", "左下角纬度", "左下角经度" };
			String[] body = { "data_id", "scene_id", "instance_id", "product_name", "satellite", "sensor",
					"spatial_resolution", "orbit_cycle", "orbit_pass", "orbit_id", "product_level", "product_format",
					"cloud_percent", "product_version", "produce_time", "time_zone", "time_type", "earth_ellipsoid",
					"project_type", "start_time", "end_time", "image_boundary", "top_left_latitude",
					"top_left_longitude", "top_right_latitude", "top_right_longitude", "bottom_right_latitude",
					"bottom_right_longitude", "bottom_left_latitude", "bottom_left_longitude" };
			//打开输出流
			out = response.getOutputStream();
			//设置类型
			response.setContentType("octets/stream");
			//设置头部信息
			response.addHeader(
					"Content-Disposition",
					"attachment;filename="
							+ DateTimeUtils.getNowStrTimeStemp() + ".xls");
			ExportExcelForShopcar.exporsOrderXls("元数据信息", data, out, DateTimeUtils.YMDHMS, head, body, local);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Export has exception, caused by "+e.getMessage(),Constant.SYSTEM);
		} finally {
			try {
				if(out!=null){
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				LOGGER.error("Export has exception, caused by "+e.getMessage(),Constant.SYSTEM);
			}
		}
	}

	/**
	 * <pre>
	 * exportXml(元数据导出XML)   
	 * 创建人：宫森      
	 * 创建时间：2018年4月9日 下午4:22:12    
	 * 修改人：宫森      
	 * 修改时间：2018年4月9日 下午4:22:12    
	 * 修改备注： 
	 * &#64;param dataList
	 * &#64;param local
	 * &#64;param request
	 * &#64;param response
	 * </pre>
	 */

	public void exportXml(List<Metadata> dataList, String local, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			response.setContentType("application/xml");
			OutputStreamWriter out = new OutputStreamWriter(response
				      .getOutputStream(), "UTF-8");
			response.setContentType("octets/stream");
			response.addHeader("Content-Disposition",
					"attachment;filename=" + DateTimeUtils.getNowStrTimeStemp() + ".xml");
			XmlUtil.BuildXmlDoc(dataList,out);
//			out.write(buildXmlDoc);
		    out.flush();
		    out.close();
		} catch (Exception e) {
			LOGGER.error("exporcsv has exception, caused by "+e.getMessage(),Constant.SYSTEM);
		}
	}

	public String object2String(Object obj) {
		String str = "";
		if (null == obj) {
			return "";
		} else {
			str = obj.toString();
		}
		return str;
	}
}
