package com.aas.mapper;

import com.aas.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 分页查询用户(带角色名称) */
    IPage<SysUser> selectUserPage(IPage<SysUser> page, @Param("q") SysUser query,
                                  @Param("roleCode") String roleCode);

    /** 批量查询用户的角色信息 */
    List<com.aas.dto.UserRoleDTO> selectRoleRowsByUserIds(@Param("userIds") List<Long> userIds);

    /** 查询用户的角色标识列表 */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /** 查询用户的角色ID列表 */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /** 查询用户的权限标识列表 */
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /** 查询用户的主角色(优先级 ADMIN>ACADEMIC>HEAD_TEACHER>STUDENT) */
    String selectMainRoleCode(@Param("userId") Long userId);
}
