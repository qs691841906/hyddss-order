package com.sinosoft.ddss.dao;

import java.math.BigDecimal;
import java.util.List;

import com.sinosoft.ddss.common.entity.Dtplan;

public interface DtplanMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ddss_dtplan
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(BigDecimal dtplanId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ddss_dtplan
     *
     * @mbg.generated
     */
    int insert(Dtplan record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ddss_dtplan
     *
     * @mbg.generated
     */
    int insertSelective(Dtplan record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ddss_dtplan
     *
     * @mbg.generated
     */
    Dtplan selectByPrimaryKey(BigDecimal dtplanId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ddss_dtplan
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(Dtplan record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ddss_dtplan
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(Dtplan record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ddss_dtplan
     *
     * @mbg.generated
     */
    List<Dtplan> getDtplanByOrderMainId(Long orderMainId);
}