package com.sinosoft.ddss.dao;

import com.sinosoft.ddss.common.entity.Role;
import com.sinosoft.ddss.common.entity.query.RoleQuery;

import java.math.BigDecimal;
import java.util.List;

public interface RoleMapper {
    
    int deleteByPrimaryKey(BigDecimal roleId);

    int insertSelective(Role record);

    Role selectByPrimaryKey(BigDecimal roleId);

    int updateByPrimaryKeySelective(Role record);

	List<Role> listRole(Role role);

	Integer countRole(RoleQuery role);

	List<Role> listRoleInit(RoleQuery roleQuery);

	Role getRoleByName(Role role);

	int updateByPrimaryKey(Role role);
	List<Role> listRoleByIds(Role role);
}