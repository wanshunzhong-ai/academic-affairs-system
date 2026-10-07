package com.aas.mapper;

import com.aas.entity.SysMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 查询用户拥有的菜单(去重, 按排序) */
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    /** 查询用户的全部权限标识 */
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /** 查询某角色拥有的菜单ID */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /** 查询全部菜单(含停用) */
    List<SysMenu> selectAllMenus();
}
