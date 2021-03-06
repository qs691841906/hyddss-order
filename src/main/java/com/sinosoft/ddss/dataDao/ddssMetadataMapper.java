package com.sinosoft.ddss.dataDao;

import java.util.List;

import com.sinosoft.ddss.common.entity.Metadata;

public interface ddssMetadataMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ddss_metadata
     *
     * @mbg.generated
     */
    int insert(Metadata record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ddss_metadata
     *
     * @mbg.generated
     */
    int insertSelective(Metadata record);
    
    
    /**
     * @param record
     * @return
     * @author li_jiazhi
     * @create 2018年3月26日上午11:11:54
     *   查询元数据详情
     */
    Metadata selectByPrimaryKey(Long dataId);
    
    /**
     * @param ids
     * @return
     * @author li_jiazhi
     * @create 2018年3月26日下午2:31:57
     *   元数据列表查询
     */
    List<Metadata> listDatas(String ids);

    /**
     * @param ids
     * @return
     * @author li_jiazhi
     * @create 2018年3月26日下午2:31:57
     *   元数据列表查询
     */
    List<Metadata> listDatasByOr(String dataIdsOr);
    
    
    /**
     * 查询n条元数据
     * @return
     */
    List<Metadata> selectAllMetadata(int n);
    
    
    /**
     * 
     * @param metadata
     * @return
     */
    int updateMetadataFileUrl(Metadata metadata);
    
    
    
}