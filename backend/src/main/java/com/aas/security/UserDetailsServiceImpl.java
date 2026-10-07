package com.aas.security;

import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.entity.StuStudent;
import com.aas.entity.SysRole;
import com.aas.entity.SysUser;
import com.aas.entity.TeaTeacher;
import com.aas.mapper.StuStudentMapper;
import com.aas.mapper.SysMenuMapper;
import com.aas.mapper.SysRoleMapper;
import com.aas.mapper.SysUserMapper;
import com.aas.mapper.TeaTeacherMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

/**
 * 用户认证信息加载
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final StuStudentMapper studentMapper;
    private final TeaTeacherMapper teacherMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
        if (user == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_EXIST);
        }
        return buildLoginUser(user);
    }

    /**
     * 按用户ID加载（JWT 认证时使用，保证权限实时生效）
     */
    public LoginUser loadUserById(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_EXIST);
        }
        return buildLoginUser(user);
    }

    /**
     * 组装登录用户上下文
     */
    public LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setRealName(user.getRealName());
        loginUser.setUserType(user.getUserType());
        loginUser.setGender(user.getGender());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setPhone(user.getPhone());
        loginUser.setEmail(user.getEmail());
        loginUser.setStatus(user.getStatus());

        // 角色
        List<String> roleCodes = userMapper.selectRoleCodesByUserId(user.getId());
        loginUser.setRoles(new HashSet<>(roleCodes));

        // 主角色 + 数据范围
        String mainRoleCode = userMapper.selectMainRoleCode(user.getId());
        loginUser.setRoleCode(mainRoleCode);
        if (mainRoleCode != null) {
            SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, mainRoleCode)
                    .last("LIMIT 1"));
            if (role != null) {
                loginUser.setRoleName(role.getRoleName());
                loginUser.setDataScope(role.getDataScope());
            }
        }

        // 权限
        loginUser.setPermissions(new HashSet<>(menuMapper.selectPermsByUserId(user.getId())));

        // 业务档案关联
        if (loginUser.isStudent()) {
            StuStudent student = studentMapper.selectByUserId(user.getId());
            if (student != null) {
                loginUser.setStudentId(student.getId());
                loginUser.setClassId(student.getClassId());
            }
        }
        if (loginUser.isHeadTeacher()) {
            TeaTeacher teacher = teacherMapper.selectByUserId(user.getId());
            if (teacher != null) {
                loginUser.setTeacherId(teacher.getId());
                loginUser.setManageClassIds(teacherMapper.selectManageClassIds(teacher.getId()));
            }
        }
        // 教务处/管理员也可能同时是教师(用于排课)，此处不额外关联

        return loginUser;
    }
}
