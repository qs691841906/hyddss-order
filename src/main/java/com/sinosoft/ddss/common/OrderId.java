package com.sinosoft.ddss.common;

import org.springframework.beans.factory.annotation.Autowired;

import com.sinosoft.ddss.dao.OrderInfoMainMapper;
import com.sinosoft.ddss.dao.OrderInfoMapper;

public class OrderId {

	@Autowired
	private OrderInfoMainMapper orderMainMapper;
	@Autowired
	private OrderInfoMapper orderInfoMapper;

	
}
