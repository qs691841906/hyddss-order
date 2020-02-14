//package com.sinosoft.ddss.mq;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.amqp.rabbit.annotation.RabbitHandler;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import com.sinosoft.ddss.common.constant.Constant;
//import com.sinosoft.ddss.common.entity.Additional;
//import com.sinosoft.ddss.common.entity.DemandOrderMain;
//import com.sinosoft.ddss.dao.DemandOrderMainMapper;
//import com.sinosoft.ddss.utils.FastJsonUtil;
//
//@Component
//@RabbitListener(queues = Constant.QUE_DEMANDORDERMAIN_TASK_PUSH)
//public class DemandOrderMainTaskPush {
//	
//	private static final  Logger log = LoggerFactory.getLogger(DemandOrderMainTaskPush.class);
//
//	@Autowired
//	private DemandOrderMainMapper demandOrderMainMapper;
//
//
//	@RabbitHandler
//	public void process(String message) {
//		try {
////			从mq中获取审核通过的订单
//			Additional orderInfoMain = FastJsonUtil.toBean(message, Additional.class);
//			if(null != orderInfoMain&&null != orderInfoMain.getOrderMainId()){
////				获取外网采集单中的信息
////				采集单号
//				Long orderMainId = orderInfoMain.getOrderMainId();
////				卫星
//				String satellite = orderInfoMain.getSatellite();
////				传感器
//				String sensor = orderInfoMain.getSensor();
////				级别
//				String outProductLevel = orderInfoMain.getOutProductLevel();
////				优先级
//				int priority = orderInfoMain.getPriority();
////				区域
//				String aoi = orderInfoMain.getAoi();
////				创建时间
//				String createTime = orderInfoMain.getCreateTime();
////				采集开始时间
//				String startTime = orderInfoMain.getStartTime();
////				采集结束时间
//				String endTime = orderInfoMain.getEndTime();
////				订单名称
//				String orderName = orderInfoMain.getOrderName();
////				工作模式
//				String workMode = orderInfoMain.getWorkMode();
////				用户单位
//				String userUnit = orderInfoMain.getUserUnit();
////				用户真实姓名
//				String realName = orderInfoMain.getRealName();
////				审核状态
//				int auditStatus = orderInfoMain.getAuditStatus();
//				
////				将采集单插入采集单表中
//				DemandOrderMain demandOrderMain = new DemandOrderMain();
//				demandOrderMain.setOrderMainId(orderMainId);
//				demandOrderMain.setSatellite(satellite);
//				demandOrderMain.setSensor(sensor);
//				demandOrderMain.setProductLevel(outProductLevel);
//				demandOrderMain.setPriority(priority);
//				demandOrderMain.setAoi(aoi);
//				demandOrderMain.setCreateTime(createTime);
//				demandOrderMain.setStartTime(startTime);
//				demandOrderMain.setEndTime(endTime);
//				demandOrderMain.setOrderName(orderName);
//				demandOrderMain.setWorkMode(workMode);
//				demandOrderMain.setUserUnit(userUnit);
//				demandOrderMain.setRealName(realName);
//				demandOrderMain.setAuditStatus(auditStatus);
////				是否为内网采集单，1是，0不是
//				demandOrderMain.setIsInner(0);
////				采集单状态（1：待处理 ，2：处理中 ，3：规划中，4：采集中的5:完成 ，6：取消，7：失败）
//				demandOrderMain.setStatus(1);
//				demandOrderMainMapper.insertDemandOrderMain(demandOrderMain);
//				
//			}
//		} catch (Exception e) {
//			log.error(e.getMessage());
//		}
//	}
//}