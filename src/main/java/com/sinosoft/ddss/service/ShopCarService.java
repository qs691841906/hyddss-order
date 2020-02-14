package com.sinosoft.ddss.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.sinosoft.ddss.common.entity.ShopInfo;
import com.sinosoft.ddss.common.entity.query.ShopInfoQuery;

public interface ShopCarService {

	/**
	 * 购物车查询列表
	 * 
	 * @return
	 */
	List<ShopInfo> ListShopInfo(ShopInfoQuery record);

	/**
	 * 购物车添加数据
	 * 
	 * @param record
	 * @return
	 */
	int insertSelective(ShopInfo record);

	/**
	 * 查询数量
	 */
	public Integer getCountByQuery(ShopInfoQuery record);

	/**
	 * 根据ID删除
	 * 
	 * @param id
	 * @return
	 */
	int deleteByPrimaryKey(String ids);

	/**
	 * 清空购物车
	 * 
	 * @return
	 */
	int delAllShopCar(String userName);

	/**
	 * 根据id查找购物车信息
	 * 
	 * @return
	 */
	List<ShopInfo> selectByShopCarIds(String dataIds);

	/**
	 * 保存到购物车
	 * @param request
	 * @param shopInfo
	 * @return
	 */
	Integer saveShopCar(ShopInfo shopInfo);

	/**
	 * 保存到购物车
	 * @param request
	 * @param shopInfo
	 * @return
	 */
	Integer saveShopCar(String dataIds, ShopInfo shopInfo);
	
	/**
	 * 根据dataId查找购物车信息
	 * 
	 * @return
	 */
	List<ShopInfo> selectByDataIds(String dataIds);
}
