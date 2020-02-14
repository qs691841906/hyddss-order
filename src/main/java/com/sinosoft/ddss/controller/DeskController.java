package com.sinosoft.ddss.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sinosoft.ddss.common.constant.Constant;
import com.sinosoft.ddss.common.entity.OrderInfo;
import com.sinosoft.ddss.common.util.JsonUtils;
import com.sinosoft.ddss.service.OrderService;

@RestController
public class DeskController {
	private static Logger LOGGER = LoggerFactory.getLogger(DeskController.class);
	@Autowired
	private OrderService orderService;
	
	@RequestMapping("/oauth/deskController/oauthHotData")
	public String hotData(HttpServletRequest request, HttpServletResponse response) {
		//getting userId or userName
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			// getting order info hot data
			List<OrderInfo> hotMap = orderService.hotData();
//			StringBuffer s = new StringBuffer();
			LOGGER.info("Hot data query successed");
			map.put("msg", "Hot data query successed");
			map.put("status", true);
			map.put("data", hotMap);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			map.put("status", false);
			map.put("msg", "Hot data query has exception");
			LOGGER.error("Hot data query has exception, caused by "+e.getMessage(),Constant.SYSTEM);
		}
		return JsonUtils.objectToJson(map);
	}
}
