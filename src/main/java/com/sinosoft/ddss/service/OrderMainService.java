package com.sinosoft.ddss.service;

import java.util.List;
import java.util.Map;

import com.sinosoft.ddss.common.entity.Additional;
import com.sinosoft.ddss.common.entity.OrderInfoMain;
import com.sinosoft.ddss.common.entity.query.OrderInfoQuery;

public interface OrderMainService {

	/**
	 * 查询订单
	 * @param orderInfo
	 * @return
	 */
	List<Additional> ListOrder(OrderInfoQuery orderInfo);
	

	/**
	 * 报警查询
	 * @param orderInfo
	 * @return
	 */
	List<Additional> QueryCallPolice(Map<String, Object> orderInfo);

	/**
	 * 查询订单数量
	 * @param orderInfo
	 * @return
	 */
	Integer getCountByQuery(OrderInfoQuery orderInfo);
	
	/**
	 * 统计各个状态的订单数量
	 * @return
	 */
	List<OrderInfoQuery> statisticalOrderMainByStatus(OrderInfoQuery orderInfo);

	/**
	 * 修改订单参数的值
	 * @param map
	 * @return
	 */
	Integer updateOrderStatus(Map<String, String> map);

	/**
	 * 审核订单
	 * @param request 
	 * @param orderInfoMain
	 * @return
	 */
	boolean auditOrder(OrderInfoQuery orderInfoQuery);

	/**
	 * 取消恢复
	 * @param orderInfoMain
	 * @return
	 */
	Map<String, Object> cancelOrder(OrderInfoMain orderInfoMain);

	/**
	 * 物理删除订单
	 * @param orderMainIds
	 * @return
	 */
	Integer delOrder(String orderMainIds);
	
	/**
	 * 获取订单id
	 * @param ordertype
	 * @return
	 */
	Long getOrderIdNew(int ordertype);

	/**
	 * 根据主订单id获取主单信息
	 * @param orderMainId
	 * @return
	 */
	Additional getOrderMainById(Long orderMainId);

	/**
	 * 修改订单
	 * @param orderInfoMain
	 * @return
	 */
	boolean updateOderMainInfo(Additional orderInfoMain);
	
	/**
	 * 查询子订单
	 * @param Map
	 * @return
	 */
	List<Map<String, Object>> findOrderByCond(Map<String, Object> condMap);

	/**
	 * 统计各个状态的订单数量采集单
	 * @return
	 */
	List<OrderInfoQuery> statisticalCollectinfoByStatus(OrderInfoQuery orderInfoQuery);
	
	/**
	 * 修改订单中已经完成了的订单状态
	 */
	void updateOrderInfoMainStatus();

}
