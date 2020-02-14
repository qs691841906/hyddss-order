package com.sinosoft.ddss.service;

import java.util.List;
import java.util.Map;

import com.sinosoft.ddss.common.entity.Additional;
import com.sinosoft.ddss.common.entity.OrderInfo;
import com.sinosoft.ddss.common.entity.query.OrderInfoQuery;

public interface OrderService {

	/**
	 * 订单的数
	 * 
	 * @param orderInfo
	 * @return
	 */
	Integer getOrderCountByQuery(OrderInfoQuery orderInfo);

	/**
	 * 购物车生成订单
	 * 
	 * @param request
	 * @param additional
	 * @return
	 */
	boolean saveOrderByShopCar(Additional additional);

	/**
	 * 购物车生成订单
	 * 
	 * @param request
	 * @param additional
	 * @return
	 */
	boolean saveOrder(Additional additional);

	/**
	 * 查询页面跳转生成订单
	 * 
	 * @param request
	 * @return
	 */
	boolean toOrderCreate(Additional additional);

	/**
	 * 根据主单id查询子单信息
	 * 
	 * @param parseLong
	 * @return
	 */
	List<OrderInfo> getOrderInfoByOrderMainId(OrderInfoQuery orderInfoQuery);

	/**
	 * 统计各个状态的订单数量
	 * 
	 * @return
	 */
	List<OrderInfoQuery> statisticalOrderByStatus(OrderInfoQuery orderInfoQuery);

	/**
	 * 修改子订单的状态
	 * 
	 * @param Map
	 * @return
	 */
	Integer updateOrderStatus(Map<String, String> params) throws Exception;

	/**
	 * 子单详情
	 * 
	 * @param parseLong
	 * @return
	 */
	OrderInfo getOrderInfoById(Long parseLong);

	/**
	 * 采集单子单信息
	 * 
	 * @param request
	 * @return
	 */
	List<Map<String, Object>> getOrderInfoByCollectionId(OrderInfoQuery orderInfoQuery);

	/**
	 * 封装订单查询条件
	 * 
	 * @param request
	 * @return
	 */
	OrderInfoQuery packagingOrderInfoQuery(OrderInfoQuery orderInfoQuery);

	/**
	 * 查询
	 * 
	 * @param request
	 * @return
	 */
	Integer getCollectinfoPlanCount(OrderInfoQuery orderInfoQuery);

	/**
	 * 热点数据
	 * 
	 * @return
	 */
	List<OrderInfo> hotData() throws Exception;

	/**
	 * 订单处理预警订单
	 * @param callPolice
	 * @return
	 */
	List<OrderInfo> QueryCallPolice(Map<String, Object> callPolice);

}
