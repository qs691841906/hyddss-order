package com.sinosoft.ddss.microService;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sinosoft.ddss.common.base.entity.Constans;
import com.sinosoft.ddss.common.entity.FtpData;
import com.sinosoft.ddss.common.entity.OrderFilter;
import com.sinosoft.ddss.common.entity.OrderInfo;
import com.sinosoft.ddss.common.entity.OrderInfoMain;
import com.sinosoft.ddss.common.entity.Task;
import com.sinosoft.ddss.common.entity.query.OrderInfoQuery;
import com.sinosoft.ddss.dao.FtpDataMapper;
import com.sinosoft.ddss.dao.OrderFilterMapper;
import com.sinosoft.ddss.dao.OrderInfoMainMapper;
import com.sinosoft.ddss.dao.OrderInfoMapper;
import com.sinosoft.ddss.dao.TaskMapper;
import com.sinosoft.ddss.utils.Command;
import com.sinosoft.ddss.utils.CopyFileUtil;
import com.sinosoft.ddss.utils.FileUtil;
import com.sinosoft.ddss.utils.Propertie;
import com.sinosoft.ddss.utils.ZipUtils;

public class FilterOrder {

	@Autowired
	private static OrderInfoMapper orderMapper;
	@Autowired
	private static TaskMapper taskMapper;
	@Autowired
	private static FtpDataMapper ftpDataMapper;
	@Autowired
	private static OrderFilterMapper orderFilterMapper;
	@Autowired
	private static OrderInfoMainMapper orderInfoMainMapper;
	
	
	/**
	 * 判断订单是否需要过滤（dataId相同，产品级别相同，链接没有失效，分发方式在线）
	 * 
	 * @param orderMainId
	 *            主单id，outProductType 定制级别
	 * @return true 过滤 ； false 不过滤
	 */
	public static boolean filter(OrderInfoQuery orderInfoQuery) {
		// 当前订单信息
		List<OrderInfo> orderInfoList = orderMapper.getOrderInfoByOrderMainIdNoPage(orderInfoQuery);
		for (OrderInfo orderInfo : orderInfoList) {
			// 查询条件
			OrderInfoQuery orderQuery = new OrderInfoQuery();
			// 订购单根据dataId查询
			if(orderInfo.getOrderType()==2){
				orderQuery.setDataId(orderInfo.getDataId());
				orderQuery.setOrderType((short)2);
				List<OrderInfo> satisfactionList = orderMapper.satisfactionOrderList(orderQuery);
				if(satisfactionList.size()>0){
					// 修改订单状态
					updateOrder(orderInfo, satisfactionList.get(0));
				}else{
					//内部订单没有。查询外部订单
					FtpData ftpData = ftpDataMapper.findByProductName(orderInfo.getProductName());
					if(ftpData==null){
						// 没有要过滤的订单是添加进任务表
						saveTask(orderInfo);
					}else{
						// 过滤订单+修改订单状态
						boolean copyResult = updateOrder(orderInfo, ftpData);
						// 过滤失败，加入任务表
						if(!copyResult){
							saveTask(orderInfo);
						}
					}
				}
			}
			// 定制单查询dataId与产品级别
			else if(orderInfo.getOrderType()==1){
				// 定制单根据dataId与输出产品级别过滤
				orderQuery.setDataId(orderInfo.getDataId());
				orderQuery.setOrderType((short)1);
				orderQuery.setOutProductLevel(orderInfo.getOutProductLevel());
				List<OrderInfo> satisfactionList = orderMapper.satisfactionOrderList(orderQuery);
				if(satisfactionList.size()>0){
					// 过滤订单+修改订单状态
					boolean updateResult = updateOrder(orderInfo, satisfactionList.get(0));
					// 过滤失败
					if(!updateResult){
						saveTask(orderInfo);
					}
				}else{
					// 没有可以过滤的订单，添加任务表
					saveTask(orderInfo);
				}
			}
			
			
			//然后查询主单号下面的所有子订单
			OrderInfoQuery orderinfoquery = new OrderInfoQuery();
			orderinfoquery.setOrderMainId(orderInfo.getOrderMainId());
			List<OrderInfo> list = orderMapper.getOrderInfoByOrderMainId(orderinfoquery);
			if(null != list && list.size()>0){
				int i = 0;
				int count = list.size();
				for (OrderInfo orderInfos : list) {
					Short orderStatus = orderInfos.getOrderStatus();
					//判断是否有1：待处理,2：处理中，如果没有,更新主单状态为3:完成
					if(1!=orderStatus&&2!=orderStatus){
						i=i+1;
					}
				}
				if(i==count){
					OrderInfoMain orderInfoMain = new OrderInfoMain();
					orderInfoMain.setOrderMainId(orderInfo.getOrderMainId());
					orderInfoMain.setOrderMainStatus((short) 5);
					orderInfoMainMapper.updateByPrimaryKeySelective(orderInfoMain);
				}
			}
			
		}
		return true;
	}
	
	@Test
	public void testProperties(){
		String key = Propertie.key("PUBLIC_DIR");
		System.out.println(key+File.separator);
	}
	
	/**
	 * 过滤的订单更新订单信息
	 * @param newOrder
	 * @param OldOrder
	 * @return
	 */
	public static boolean updateOrder(OrderInfo newOrder, OrderInfo oldOrder){
		try {
			if(oldOrder.getDownloadStatus()==1){
				OrderFilter orderFilter = new OrderFilter();
				orderFilter.setOldOrderId(oldOrder.getOrderId());
				orderFilter.setNewOrderId(newOrder.getOrderId());
				orderFilter.setStatus((short)0);
				orderFilterMapper.insert(orderFilter);
			}else{
				// 原文件地址
				StringBuilder oldDir = new StringBuilder(Propertie.key("PUBLIC_DIR"));
				oldDir.append(oldOrder.getProDownloadAdd());
				oldDir.append(oldOrder.getProductName());
				// 产品目录
				String oldProFile = oldDir.toString() + Propertie.key("PRODUCT_SUF");
				// 质检报告地址
				String oldRepFile = oldDir.toString() + Propertie.key("REPORT_SUF");
				// 最终地址
				StringBuilder newDir = new StringBuilder(Propertie.key("PUBLIC_DIR"));
				newDir.append(newOrder.getProDownloadAdd());
				newDir.append(oldOrder.getProductName());
				// 产品目录
				String newProFile = newDir.toString() + Propertie.key("PRODUCT_SUF");
				// 质检报告地址
				String newRepFile = newDir.toString() + Propertie.key("REPORT_SUF");
				// 拷贝数据包与质检报告包
//				CopyFileUtil.copyFile(oldProFile, newProFile);
//				CopyFileUtil.copyFile(oldRepFile, newRepFile);
				
				String commandStr = "ln -s "+oldProFile+" "+newProFile;
				Command.exeCmd(commandStr);
				
				String commandStr2 = "ln -s "+oldRepFile+" "+newRepFile;
				Command.exeCmd(commandStr2);
				
				// 更新订单信息
				OrderInfo orderInfo = new OrderInfo(newOrder.getOrderId());
				orderInfo.setProductName(oldOrder.getProductName());
				orderInfo.setDataSize(oldOrder.getDataSize());
				// 下载状态：等待下载
				orderInfo.setDownloadStatus((short)2);
//			orderInfo.setProDownloadAdd(newProFile);
//			orderInfo.setRepDownloadAdd(newRepFile);
				// 订单状态：待分发
				orderInfo.setOrderStatus((short)3);
				orderMapper.updateByPrimaryKeySelective(orderInfo);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	/**
	 * 过滤的订单更新订单信息
	 * @param newOrder
	 * @param OldOrder
	 * @return
	 */
	public static boolean updateOrder(OrderInfo newOrder, FtpData ftpData){
		String productName = ftpData.getProductName();
		// 公共目录
		StringBuilder oldDir = new StringBuilder(Propertie.key("PUBLIC_DIR"));
		// 外网分发数据
		oldDir.append(Propertie.key("OUT_ORDER_DIR"));
		oldDir.append(File.separator);
		// 拼接用户名
		oldDir.append(ftpData.getRealName());
		oldDir.append(File.separator);
		// 拼接卫星
		oldDir.append(ftpData.getSatellite());
		oldDir.append(File.separator);
		// 拼接传感器
		oldDir.append(ftpData.getSensor());
		oldDir.append(File.separator);
		// 拼接包名
		oldDir.append(productName);
		// 当前用户名目录
		StringBuilder newDir = new StringBuilder(Propertie.key("PUBLIC_DIR"));
		newDir.append(newOrder.getProDownloadAdd());
		newDir.append(newOrder.getProductName());
		try {
			// 解压包后有5个文件，A文件夹内有两个质检三个产品，将质检取出放到B文件夹，压缩B，生成质检包，将A内的质检删除压缩A生成产品包
			ZipUtils.unZip(oldDir.toString()+Propertie.key("PRODUCT_SUF"),newDir.toString());
			FileUtil.copyFile(newDir.toString()+Propertie.key("REPORT_SUF1"), newDir.toString()+"QC");
			FileUtil.copyFile(newDir.toString()+Propertie.key("REPORT_SUF2"), newDir.toString()+"QC");
			FileUtil.deleteFile(newDir.toString()+Propertie.key("REPORT_SUF1"));
			FileUtil.deleteFile(newDir.toString()+Propertie.key("REPORT_SUF2"));
			ZipUtils.zipFolderFile(newOrder.getProductName()+Propertie.key("PRODUCT_SUF"), newDir.toString(), false);
			ZipUtils.zipFolderFile(newOrder.getProductName()+Propertie.key("REPORT_SUF"), newDir.toString(), false);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * 加入任务表
	 * @param orderInfo
	 * @return
	 */
	public static boolean saveTask(OrderInfo orderInfo){
		Task task = new Task();
		task.setOrderId(orderInfo.getOrderId());
		task.setOrderMainId(orderInfo.getOrderMainId());
		task.setSender(Constans.DDSS);
		if(orderInfo.getOrderType()==1){
			task.setReceiver(Constans.OCCS);
		}else {
			task.setReceiver(Constans.DASS);
		}
		task.settaskType(1);
		taskMapper.saveTask(task);
		return true;
	}
	
	
	
	
	
	
	
}
