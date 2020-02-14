package com.sinosoft.ddss.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sinosoft.ddss.common.entity.Metadata;
import com.sinosoft.ddss.common.entity.ShopInfo;
import com.sinosoft.ddss.common.entity.User;
import com.sinosoft.ddss.common.entity.query.ShopInfoQuery;
import com.sinosoft.ddss.common.util.DateTimeUtils;
import com.sinosoft.ddss.dao.ShopInfoMapper;
import com.sinosoft.ddss.dataDao.ddssMetadataMapper;
import com.sinosoft.ddss.jedis.JedisClient;
import com.sinosoft.ddss.service.DecryptToken;
import com.sinosoft.ddss.service.ShopCarService;

@Service
public class ShopCarSerciceImpl implements ShopCarService {

	@Autowired
	private ShopInfoMapper shopInfoMapper;
	@Autowired
	private ddssMetadataMapper metadataQueryMapper;
	@Autowired
	private DecryptToken decryptToken;
	@Autowired
	private JedisClient jedisClient;

	/**
	 * 查询购物车列表
	 */
	@Override
	public List<ShopInfo> ListShopInfo(ShopInfoQuery record) {
		List<ShopInfo> shopCarList = shopInfoMapper.listShopInfo(record);
		String imageIp = jedisClient.get("imageIp");
		String imagePort = jedisClient.get("imagePort");
		for (ShopInfo shopInfo : shopCarList) {
			if(shopInfo.getThumbFileUrl()!=null){
				shopInfo.setThumbFileUrl(imageIp+imagePort+"/"+shopInfo.getThumbFileUrl());
			}
			if(shopInfo.getQuickFileUrl()!=null){
				shopInfo.setQuickFileUrl(imageIp+imagePort+"/"+shopInfo.getQuickFileUrl());
			}
		}
		return shopCarList;
	}

	/**
	 * 新增购物车数据
	 */
	@Override
	public int insertSelective(ShopInfo record) {
		record.setCreateTime(DateTimeUtils.getNowStrTime());
		return shopInfoMapper.insertSelective(record);
	}

	/**
	 * 查询购物车数量
	 */
	@Override
	public Integer getCountByQuery(ShopInfoQuery record) {
		return shopInfoMapper.getCountByQuery(record);
	}

	/**
	 * in删除
	 */
	@Override
	public int deleteByPrimaryKey(String ids) {
		return shopInfoMapper.deleteByPrimaryKey(ids);
	}

	/**
	 * 清空购物车
	 */
	@Override
	public int delAllShopCar(String userName) {
		return shopInfoMapper.delAllShopCar(userName);
	}

	/**
	 * 根据id查找购物车信息
	 */
	@Override
	public List<ShopInfo> selectByShopCarIds(String dataIds) {
		List<ShopInfo> listShopCar = shopInfoMapper.selectByPrimaryKeys(dataIds);
		String imageIp = jedisClient.get("imageIp");
		String imagePort = jedisClient.get("imagePort");
		for (ShopInfo shopInfo : listShopCar) {
			if(shopInfo.getThumbFileUrl()!=null){
				shopInfo.setThumbFileUrl(imageIp+":"+imagePort+"/"+shopInfo.getThumbFileUrl());
			}
			if(shopInfo.getQuickFileUrl()!=null){
				shopInfo.setQuickFileUrl(imageIp+":"+imagePort+"/"+shopInfo.getQuickFileUrl());
			}
		}
		return listShopCar;
	}

	/**
	 * 添加购物车
	 */
	@Override
	public Integer saveShopCar(ShopInfo shopInfo) {
		// 获取数据id
		String dataIds =shopInfo.getDataIds() ;
		List<Metadata> listDatas = metadataQueryMapper.listDatas(dataIds);
		if(listDatas.size()<=0)
			return 0;
		User user = decryptToken.decyptToken(shopInfo.getToken());
		//购物车数据
		List<ShopInfo> list = new ArrayList<ShopInfo>();
		for (Metadata metadata : listDatas) {
			Short orderType = shopInfo.getOrderType();
			ShopInfo shopInfo2 = new ShopInfo();
			shopInfo2.setDataId(metadata.getDataId());
			shopInfo2.setOrderType(orderType);
			shopInfo2.setSatellite(metadata.getSatellite());
			shopInfo2.setSensor(metadata.getSensor());
			shopInfo2.setCollectionTime(metadata.getImageStartTime());
			shopInfo2.setProductionTime(metadata.getProduceTime());
			shopInfo2.setProductLevel(metadata.getProductLevel());
			shopInfo2.setOutProductLevel(shopInfo.getProductLevel());
			shopInfo2.setCloudCoverage(metadata.getCloudPercent());
			shopInfo2.setUserName(user.getUserName());
			shopInfo2.setThumbFileUrl(metadata.getThumbFileUrl());
			shopInfo2.setQuickFileUrl(metadata.getQuickFileUrl());
			shopInfo2.setProductName(metadata.getProductName());
			shopInfo2.setDataSize(metadata.getDataSize());
			shopInfo2.setImageStartTime(metadata.getImageStartTime());
			shopInfo2.setImageEndTime(metadata.getImageEndTime());
			list.add(shopInfo2);
		}
		return shopInfoMapper.saveShopInfo(list);
	}
	
	/**
	 * 外部接口
	 */
	@Override
	public Integer saveShopCar(String dataIds, ShopInfo shopInfo) {
		// 获取数据id
		List<Metadata> listDatas = metadataQueryMapper.listDatas(dataIds.replace("|", ","));
		if(listDatas.size()<=0)
			return 0;
		//购物车数据
		List<ShopInfo> list = new ArrayList<ShopInfo>();
		for (Metadata metadata : listDatas) {
			Short orderType = shopInfo.getOrderType();
			ShopInfo shopInfo2 = new ShopInfo();
			shopInfo2.setDataId(metadata.getDataId());
			shopInfo2.setOrderType(orderType);
			shopInfo2.setSatellite(metadata.getSatellite());
			shopInfo2.setSensor(metadata.getSensor());
			shopInfo2.setCollectionTime(metadata.getImageStartTime());
			shopInfo2.setProductionTime(metadata.getProduceTime());
			shopInfo2.setProductLevel(metadata.getProductLevel());
			shopInfo2.setOutProductLevel(null == shopInfo.getProductLevel()?metadata.getProductLevel():shopInfo.getProductLevel());
			shopInfo2.setCloudCoverage(metadata.getCloudPercent());
			shopInfo2.setUserName(shopInfo.getUserName());
			shopInfo2.setProductName(metadata.getProductName());
			shopInfo2.setDataSize(metadata.getDataSize());
			list.add(shopInfo2);
		}
		return shopInfoMapper.saveShopInfo(list);
	}
	
	/**
	 * 根据id查找购物车信息
	 */
	@Override
	public List<ShopInfo> selectByDataIds(String dataIds) {
		List<ShopInfo> listShopCar = shopInfoMapper.selectByDataIds(dataIds);
		return listShopCar;
	}
}
