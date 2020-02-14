package com.sinosoft.ddss.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sinosoft.ddss.common.base.entity.Constans;
import com.sinosoft.ddss.common.entity.OrderInfo;
import com.sinosoft.ddss.common.entity.OrderInfoMain;
import com.sinosoft.ddss.common.entity.Task;
import com.sinosoft.ddss.common.entity.query.OrderInfoQuery;
import com.sinosoft.ddss.dao.OrderInfoMapper;
import com.sinosoft.ddss.dao.TaskMapper;
import com.sinosoft.ddss.service.OrderFilterService;
import com.sinosoft.ddss.service.TaskService;

@Service
public class TaskServiceImpl implements TaskService {

	@Autowired
	private TaskMapper taskMapper;
	@Autowired
	private OrderInfoMapper orderInfoMapper;
	@Autowired
	private OrderFilterService orderFilter;

	@Override
	public int saveTask(Task task) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int saveTaskList(List<Task> taskList) {
		taskMapper.saveTaskList(taskList);
		return 0;
	}

	/**
	 * 根据主订单信息添加任务表
	 */
	@Override
	public void saveTaskByOrderMain(OrderInfoMain orderInfoMain) {
		// 获取订单类型
		short orderType = orderInfoMain.getOrderType();
		switch (orderType) {
		case 1:// 普通订购
				// 根据主单获取子单信息，添加进任务表
			OrderInfoQuery orderInfoQuery = new OrderInfoQuery();
			orderInfoQuery.setStartNum(0);
			orderInfoQuery.setPageSize(10000);
			orderInfoQuery.setOrderMainId(orderInfoMain.getOrderMainId());
			List<OrderInfo> orderInfoList = orderInfoMapper.getOrderInfoByOrderMainId(orderInfoQuery);
			List<Task> list = new ArrayList<Task>();
			for (OrderInfo orderInfo : orderInfoList) {
				Boolean filter = orderFilter.OrderFilter(orderInfo);
				if(!filter){
					Task task = new Task();
					task.setOrderMainId(orderInfoMain.getOrderMainId());
					task.setOrderId(orderInfo.getOrderId());
					task.setSender(Constans.DDSS);
					task.setReceiver(Constans.DASS);
					task.settaskType(1);
					list.add(task);
				}
			}
			taskMapper.saveTaskList(list);
			break;
		case 2:// 快速订购
			Task task2 = new Task();
			task2.setOrderMainId(orderInfoMain.getOrderMainId());
			task2.setSender(Constans.DDSSO);
			task2.setReceiver(Constans.DDSSI);
			task2.settaskType(1);
			taskMapper.saveTask(task2);
			break;
		case 3:// 普通定制
			List<OrderInfo> orderInfoList3 = orderInfoMapper.getOrderInfoByOrderMainId(new OrderInfoQuery(orderInfoMain.getOrderMainId()));
			List<Task> list3 = new ArrayList<Task>();
			for (OrderInfo orderInfo : orderInfoList3) {
				Boolean filter = orderFilter.OrderFilter(orderInfo);
				if(!filter){
					Task task3 = new Task();
					task3.setOrderMainId(orderInfoMain.getOrderMainId());
					task3.setOrderId(orderInfo.getOrderId());
					task3.setSender(Constans.DDSS);
					task3.setReceiver(Constans.OCCS);
					task3.settaskType(2);
					list3.add(task3);
				}
			}
			taskMapper.saveTaskList(list3);
			break;
		case 4:// 批量定制
//			Task task4 = new Task();
//			task4.setOrderMainId(orderInfoMain.getOrderMainId());
//			task4.setSender(Constans.DDSS);
//			task4.setReceiver(Constans.OCCS);
//			task4.settaskType(1);
//			taskMapper.saveTask(task4);
//			break;
			
			List<OrderInfo> orderInfoList4 = orderInfoMapper.getOrderInfoByOrderMainId(new OrderInfoQuery(orderInfoMain.getOrderMainId()));
			List<Task> list4 = new ArrayList<Task>();
			for (OrderInfo orderInfo : orderInfoList4) {
				Boolean filter = orderFilter.OrderFilter(orderInfo);
				if(!filter){
					Task task3 = new Task();
					task3.setOrderMainId(orderInfoMain.getOrderMainId());
					task3.setOrderId(orderInfo.getOrderId());
					task3.setSender(Constans.DDSS);
					task3.setReceiver(Constans.OCCS);
					task3.settaskType(2);
					list4.add(task3);
				}
			}
			taskMapper.saveTaskList(list4);
			break;
			
		case 5:// 普通采集
			
			break;
		case 6:// 快速采集

			break;
		case 7:// 订购定制
			List<OrderInfo> orderInfoList7 = orderInfoMapper.getOrderInfoByOrderMainId(new OrderInfoQuery(orderInfoMain.getOrderMainId()));
			List<Task> list7 = new ArrayList<Task>();
			for (OrderInfo orderInfo : orderInfoList7) {
				Boolean filter = orderFilter.OrderFilter(orderInfo);
				if(!filter){
					Task task = new Task();
					task.setOrderMainId(orderInfoMain.getOrderMainId());
					task.setOrderId(orderInfo.getOrderId());
					task.setSender(Constans.DDSS);
					if(orderInfo.getOrderType()==1){
						task.setReceiver(Constans.DASS);
					}else if(orderInfo.getOrderType()==1){
						task.setReceiver(Constans.OCCS);
					}
					task.settaskType(1);
					list7.add(task);
				}
			}
			taskMapper.saveTaskList(list7);
			break;
		default:
			break;
		}
	}

}
