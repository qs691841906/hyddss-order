package com.sinosoft.ddss.service.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sinosoft.ddss.common.entity.Additional;
import com.sinosoft.ddss.common.entity.OrderInfo;
import com.sinosoft.ddss.common.entity.OrderInfoMain;
import com.sinosoft.ddss.common.entity.query.OrderInfoQuery;
import com.sinosoft.ddss.common.util.DateTimeUtils;
import com.sinosoft.ddss.dao.OrderInfoMainMapper;
import com.sinosoft.ddss.dao.OrderInfoMapper;
import com.sinosoft.ddss.service.OrderFilterService;
import com.sinosoft.ddss.utils.Command;
import com.sinosoft.ddss.utils.Constants;
import com.sinosoft.ddss.utils.CopyFileUtil;
import com.sinosoft.ddss.utils.Propertie;

@Service
public class OrderFilterServiceImpl implements OrderFilterService {
	
	
	private static Logger log = LoggerFactory.getLogger(OrderFilterServiceImpl.class);
	@Autowired
	private OrderInfoMainMapper orderInfoMainMapper;
	@Autowired
	private OrderInfoMapper orderInfoMapper;	
	
	
	
	@Override
	public Boolean OrderFilter(OrderInfo orderInfo) {
		Boolean flag = false;
//		先判断是否为在线订单
		OrderInfoMain orderInfoMain = orderInfoMainMapper.getOrderMainById(orderInfo.getOrderMainId());
		Short distributionType = orderInfoMain.getDistributionType();
//		分发方式（1：在线，2：专线，3：离线）
//		如果不是在线订单就不用过滤
		if(null==distributionType||1!=distributionType){
			flag = false;
			return flag;
		}
//		判断订单类型（采集单不用过滤）
		Short orderType = orderInfo.getOrderType();
//		订单类型（1：订购。2：订制，3：采集）
//		采集单不用过滤
		if(null==orderType||3==orderType){
			flag = false;
			return flag;
		}else if(orderType==1){
//			过滤在线订购单
			flag = OrderOrderFilter(orderInfo);
		}else if(orderType==2){
//			过滤在线订制单	（后经讨论定制暂不过滤）
//			flag = CustomOrderFilter(orderInfo);
		}else{
			log.error("订单号为："+orderInfo.getOrderId()+"的订单存在异常订单类型，订单类型为"+orderType);
			flag = false;
			return flag;
		}
		
		return flag;
	}




	@Override
	public Boolean OrderOrderFilter(OrderInfo orderInfo) {
		boolean flag = false;
		OrderInfoQuery p_orderInfo = new OrderInfoQuery();
//		查询dataId相同且订单状态为完成和待分发的订单,并拷贝数据修改状态
		p_orderInfo.setDataId(orderInfo.getDataId());
//		状态为-1表示完成和待分发
		p_orderInfo.setOrderStatus(-1);
		flag = FindOrderAndCopyFile(orderInfo,p_orderInfo);
//		如果没有成功就将订单插入人物表
		return flag;
	}




	@Override
	public Boolean CustomOrderFilter(OrderInfo orderInfo) {
		boolean flag = false;
		OrderInfoQuery p_orderInfo = new OrderInfoQuery();
//		查询dataId相同且订单状态为完成和待分发的订单，并拷贝数据修改状态
		p_orderInfo.setDataId(orderInfo.getDataId());
//		状态为-1表示完成和待分发
		p_orderInfo.setOrderStatus(-1);
		p_orderInfo.setOutProductLevel(orderInfo.getOutProductLevel());
		flag = FindOrderAndCopyFile(orderInfo,p_orderInfo);
		if(flag){
			return flag;
		}
//		如果没有成功继续查询处理中的订单
		p_orderInfo.setOrderStatus(2);
		List<OrderInfo> list_orderInfo = orderInfoMapper.getOrderInfoByDataId(p_orderInfo);
//		如果有处理中的订单就将本订单状态改为处理中，在其他相同订单完成时同步修改本订单状态并拷贝数据
		if(null==list_orderInfo||list_orderInfo.size()<=0){
			flag = false;
			return flag;
		}else{
			OrderInfo p_orderInfo2 = new OrderInfo();
			p_orderInfo2.setOrderId(orderInfo.getOrderId());
			p_orderInfo2.setOrderStatus((short) 2);
			int i = orderInfoMapper.updateByPrimaryKey(p_orderInfo2);
			if(i<1){
				log.error("订单"+orderInfo.getOrderId()+"过滤后修改状态失败");
				return false;
			}else{
				flag = true;
			}
		}
		return flag;
	}



	@Override
	public Boolean FindOrderAndCopyFile(OrderInfo orderInfo, OrderInfoQuery orderInfoQuery) {
		
		boolean flag = false;
		List<OrderInfo> list_orderInfo = orderInfoMapper.getOrderInfoByDataId(orderInfoQuery);
//		获取相同订单的下载链接
		if(null==list_orderInfo||list_orderInfo.size()<=0){
			flag = false;
			return flag;
		}
//		拷贝质检报告（质检报告拷贝失败不影响订单过滤）
		boolean flag2 = false;
//		循环拷贝数据
		for(OrderInfo r_orderInfo:list_orderInfo){
			
		
		
		
		
//		OrderInfo r_orderInfo = list_orderInfo.get(0);
		String dds = "DDS_CACHE";
		String userName = r_orderInfo.getUserName();
		String proDownloadAdd = r_orderInfo.getProDownloadAdd();
		String productName = r_orderInfo.getProductName();
		
		String url = proDownloadAdd+productName;
		
		// 产品目录
		String oldProFile = File.separator+dds+File.separator+userName+url + Propertie.key("PRODUCT_SUF");
		// 质检报告地址
		String oldRepFile = File.separator+dds+File.separator+userName+url + Propertie.key("REPORT_SUF");
		
		String newUserName = orderInfo.getUserName();
		
		String newProFile = File.separator+dds+File.separator+newUserName+url + Propertie.key("PRODUCT_SUF");
		String newRepFile = File.separator+dds+File.separator+newUserName+url + Propertie.key("REPORT_SUF");
		
//		根据下载链接去拷贝压缩包
//		拷贝产品包
		File oldfile = new File(oldProFile);
		File newfile_ = new File(newRepFile);
		if (newfile_.exists()) { // 如果新文件存在时
			flag = true;
		
		}else if (oldfile.exists()) { // 文件存在时
			if(!oldProFile.equals(newProFile)){
				File newFile = new File(newProFile);
				File parentFile = newFile.getParentFile();
//				先创建新文件的上级路径
				if(!parentFile.exists()){
					parentFile.mkdirs();
				}
			File linkFileName = new File("");
				try {
//					flag = CopyFileUtil.copyFile(oldProFile,newProFile);
					Path targetFile = Paths.get(oldProFile);
					Path linkFile = Paths.get(newProFile);
					Path linkPath =	Files.createLink(linkFile, targetFile);
					linkFileName = linkPath.toFile();
					log.info("硬链接拷贝目标路径："+linkFileName.getPath());
//					Files.createLink(oldProFile, newProFile);
//					String commandStr = "ln -s "+oldProFile+" "+newProFile;
//					Command.exeCmd(commandStr);
//					flag = true;
				} catch (Exception e) {
					flag = false;
					log.error(e.getMessage());
				}
//				File newfile = new File(newProFile);
				if (linkFileName.exists()) { // 文件存在时
					flag = true;
				}else{
					flag = false;
				}
			
			}else{
				flag = true;
			}
		}else{
			flag = false;
			log.info("源文件不存在，源文件路径："+oldProFile);
		}
		if(flag){
			log.info("订单号为："+orderInfo.getOrderId()+"订单过滤，数据产品压缩包拷贝成功,旧路径："+oldProFile+"新路径："+newProFile);
//			如果拷贝成功就继续执行
		}else{
			log.info("订单号为："+orderInfo.getOrderId()+"订单过滤，数据产品压缩包拷贝失败,旧路径："+oldProFile+"新路径："+newProFile);
//			return flag;
//			如果拷贝失败就再次循环
			continue;
			
		}
		

			
			File oldfile_rep = new File(oldRepFile);
			
			File newfile_rep = new File(newRepFile);
			
			if (newfile_rep.exists()) { // 如果新文件存在时
				flag2 = true;
			
			}else if (oldfile_rep.exists()) { // 文件存在时
				
				if(!oldRepFile.equals(newRepFile)){
					
					File newFile = new File(newRepFile);
					File parentFile = newFile.getParentFile();
//					先创建新文件的上级路径
					if(!parentFile.exists()){
						parentFile.mkdirs();
					}
					
					File linkFileName = new File("");
				try {
					
					Path targetFile = Paths.get(oldRepFile);
					Path linkFile = Paths.get(newRepFile);
					Path linkPath =	Files.createLink(linkFile, targetFile);
					linkFileName = linkPath.toFile();
					log.info("硬链接拷贝目标路径："+linkFileName.getPath());
					
//					flag2 = CopyFileUtil.copyFile(oldRepFile,newRepFile);
//					String commandStr = "ln -s "+oldRepFile+" "+newRepFile;
//					Command.exeCmd(commandStr);
					
	//				 Runtime.getRuntime()
	//				 .exec(commandStr).waitFor();
					
//					flag = true;
				} catch (Exception e) {
					flag2 = false;
					log.error(e.getMessage());
				}
				
				File newfile = new File(newRepFile);
				if (newfile.exists()) { // 文件存在时
					flag2 = true;
				}else{
					flag2 = false;
				}
					
				}else{
					flag2 = true;
				}
			}else{
				flag2 = false;
			}
		if(flag2){
			log.info("订单号为："+orderInfo.getOrderId()+"订单过滤，质检报告压缩包拷贝成功,旧路径："+oldRepFile+"新路径："+newRepFile);
			break;
		}else{
			log.info("订单号为："+orderInfo.getOrderId()+"订单过滤，质检报告压缩包拷贝失败,旧路径："+oldRepFile+"新路径："+newRepFile);
//			return flag;
			break;
		}
	}
//		如果拷贝成功就修改订单状态为待分发
		OrderInfo p_orderInfo2 = new OrderInfo();
		p_orderInfo2.setOrderId(orderInfo.getOrderId());
		p_orderInfo2.setOrderStatus((short) 4);
		p_orderInfo2.setDownloadStatus((short) 2);
		
		String date = DateTimeUtils.DateToString(new Date(), Constants.YMD);
		String orderStep = ",2_"+date+",3_"+date;
		
		p_orderInfo2.setOrderStep(orderStep);
		
//		p_orderInfo2.setOrderStep("2_"+""+"3_"+"");
		if(flag2){
			p_orderInfo2.setRepStatus(1);
		}
		int i = orderInfoMapper.updateByPrimaryKey(p_orderInfo2);
		
		if(i<1){
			log.error("订单"+orderInfo.getOrderId()+"过滤后修改状态失败");
			return false;
		}else{
			flag = true;
//			当子单过滤成功后，主单判断所有子单是否都完成，都完成修改状态为已完成，否则修改状态为处理中
			Long orderMainId = orderInfo.getOrderMainId();
//			OrderInfoMain orderInfoMain = orderInfoMainMapper.getOrderMainById(orderMainId);
			OrderInfoQuery orderInfoQuery1 = new  OrderInfoQuery();
			orderInfoQuery1.setOrderMainId(orderMainId);
			List<OrderInfo> list_order = orderInfoMapper.getOrderInfoByOrderMainId(orderInfoQuery1);
			int count_order = list_order.size();
//			完成的订单数
			int finsh_count = 0;
			if(count_order>0){
				for(OrderInfo orderInfo1:list_order){
					int i1 = orderInfo1.getOrderStatus();
//					统计完成的订单数
					if(i1==3||i1==4){
						finsh_count = finsh_count+1;
					}
				}
				if(count_order==finsh_count){
//					将主单状态修改为已完成5
					Map<String,String> map = new HashMap<String,String>();
					map.put("orderId", String.valueOf(orderMainId));
					map.put("orderStatus", "5");
					orderInfoMainMapper.updateOrderStatus(map);
				}else {
					Additional additional = orderInfoMainMapper.getOrderMainById(orderMainId);
					String dataStartTime = additional.getDataStartTime();
					
//					将主单状态修改为处理中2
					Map<String,String> map = new HashMap<String,String>();
					map.put("orderId", String.valueOf(orderMainId));
					map.put("orderStatus", "2");
//					如果数据准备开始时间是空的，就说明是第一次添加该时间
					if(StringUtils.isBlank(dataStartTime)){
						map.put("dataStartTime", DateTimeUtils.DateToString(new Date(), Constants.YMD));
					}
					orderInfoMainMapper.updateOrderStatus(map);
					
				}
			}
		}
		return flag;
	}
	
}
