package com.sinosoft.ddss.dao;

import java.util.List;

import com.sinosoft.ddss.common.entity.EmailTask;
import com.sinosoft.ddss.common.entity.query.EmailTaskQuery;


public interface EmailTaskMapper {

	int deleteByPrimaryKey(Long id);

	int insert(EmailTask record);

	int insertSelective(EmailTask record);

	EmailTask selectByPrimaryKey(Long id);

	int updateByPrimaryKeySelective(EmailTask record);

	int updateByPrimaryKey(EmailTask record);

	List<EmailTask> listTask(EmailTask task);

	List<EmailTask> listEmailTask(EmailTaskQuery emailTask);

	Integer countTask(EmailTaskQuery emailTask);

	int updateEmailStatus(String status,String ids);
}