package com.sinosoft.ddss.mq;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sinosoft.ddss.common.constant.Constant;
import com.sinosoft.ddss.common.entity.OrderInfoMain;
import com.sinosoft.ddss.dao.OrderInfoMainMapper;
import com.sinosoft.ddss.service.impl.TaskServiceImpl;
import com.sinosoft.ddss.utils.FastJsonUtil;

@Component
@RabbitListener(queues = Constant.QUE_ORDERMAIN_TASK_PUSH)
public class OrderMainTaskPush {
	
	private static final  Logger log = LoggerFactory.getLogger(OrderMainTaskPush.class);

	@Autowired
	private OrderInfoMainMapper orderMainMapper;
	@Autowired
	private TaskServiceImpl taskServiceImpl;

	@RabbitHandler
	public void process(String message) {
		try {
//			从mq中获取审核通过的订单
			OrderInfoMain orderInfoMain = FastJsonUtil.toBean(message, OrderInfoMain.class);
			if(null != orderInfoMain&&null != orderInfoMain.getOrderMainId()){
//				将订单插入任务表中
				List<OrderInfoMain> orderMainList = orderMainMapper.listOrderByOrderMainIds(String.valueOf(orderInfoMain.getOrderMainId()));
				for (int i = 0; i < orderMainList.size(); i++) {
					taskServiceImpl.saveTaskByOrderMain(orderMainList.get(i));
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}
}