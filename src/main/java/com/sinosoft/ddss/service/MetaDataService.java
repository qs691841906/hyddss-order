package com.sinosoft.ddss.service;

import com.sinosoft.ddss.common.entity.Metadata;

public interface MetaDataService {

	Metadata selectByPrimaryKey(Long dataId);
}
