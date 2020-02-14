package com.sinosoft.ddss.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sinosoft.ddss.common.entity.Metadata;
import com.sinosoft.ddss.dataDao.ddssMetadataMapper;
import com.sinosoft.ddss.service.MetaDataService;

@Service
public class MetaDataServiceImpl implements MetaDataService {

	@Autowired
	private ddssMetadataMapper metadataQueryMapper;

	@Override
	public Metadata selectByPrimaryKey(Long dataId) {
		Metadata metadata = metadataQueryMapper.selectByPrimaryKey(dataId);
		return metadata;
	}

}
