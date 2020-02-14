package com.sinosoft.ddss.service;

import com.sinosoft.ddss.common.entity.OrderInfo;
import com.sinosoft.ddss.common.entity.query.OrderInfoQuery;

public interface OrderFilterService {

	
	/**
	 * @author josen
	 * 订单过滤（只过滤在线订单，订购只过滤有完成的订单，定制还需要过滤处理中的订单）
	 * @param orderInfo
	 * @return true：过滤，不插入任务表。false：不过滤，插入人物表
	 */
	Boolean OrderFilter(OrderInfo orderInfo);
	
	/**
	 * @author josen
	 * 订购订单过滤
	 * @param orderInfo
	 * @return true：过滤，不插入任务表。false：不过滤，插入人物表
	 */
	Boolean OrderOrderFilter(OrderInfo orderInfo);
	

	/**
	 * @author josen
	 * 定制订单过滤
	 * @param orderInfo
	 * @return true：过滤，不插入任务表。false：不过滤，插入人物表
	 */
	Boolean CustomOrderFilter(OrderInfo orderInfo);
	
	
	/**
	 * 根据条件查询订单并拷贝文件
	 * @param orderInfo
	 * @return
	 */
	Boolean FindOrderAndCopyFile(OrderInfo orderInfo,OrderInfoQuery orderInfoQuery);
	
	
}
