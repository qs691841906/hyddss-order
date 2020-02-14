package com.sinosoft.ddss.service;

import java.util.List;

import com.sinosoft.ddss.common.entity.OrderInfoMain;
import com.sinosoft.ddss.common.entity.Task;

public interface TaskService {

	/**
	 * 新增任务表
	 * @param task
	 * @return
	 */
	int saveTask(Task task);

	/**
	 * 批量新增任务表
	 * @param task
	 * @return
	 */
	int saveTaskList(List<Task> task);
	
	/**
	 * 根据主单新增任务表记录
	 * @param task
	 * @return
	 */
	void saveTaskByOrderMain(OrderInfoMain orderInfoMain);
}
