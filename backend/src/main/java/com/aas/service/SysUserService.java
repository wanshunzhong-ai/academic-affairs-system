package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.dto.UserRoleDTO;
import com.aas.dto.query.UserQuery;
import com.aas.entity.SysUser;
import com.aas.entity.SysUserRole;
import com.aas.mapper.SysUserMapper;
import com.aas.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统用户服务
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    public static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询
     */
    public PageResult<SysUser> page(UserQuery query) {
        SysUser condition = new SysUser();
        condition.setUsername(query.getKeyword());
        condition.setUserType(query.getUserType());
        condition.setStatus(query.getStatus());

        Page<SysUser> page = query.toPage();
        var result = userMapper.selectUserPage(page, condition, query.getRoleCode());
        fillRoles(result.getRecords());
        return PageResult.of(result);
    }

    public SysUser detail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        user.setRoleIds(userMapper.selectRoleIdsByUserId(id));
        List<SysUser> list = new ArrayList<>();
        list.add(user);
        fillRoles(list);
        return user;
    }

    /**
     * 新增用户
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysUser user, List<Long> roleIds) {
        checkUsernameUnique(user.getUsername(), null);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(DEFAULT_PASSWORD);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getUserType() == null) {
            user.setUserType("ADMIN");
        }
        userMapper.insert(user);
        saveRoles(user.getId(), roleIds);
        return user.getId();
    }

    /**
     * 修改用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(SysUser user, List<Long> roleIds) {
        SysUser exists = userMapper.selectById(user.getId());
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        checkUsernameUnique(user.getUsername(), user.getId());
        // 密码不在此接口修改
        user.setPassword(null);
        userMapper.updateById(user);
        if (roleIds != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
            saveRoles(user.getId(), roleIds);
        }
    }

    /**
     * 删除用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        if ("admin".equals(user.getUsername())) {
            throw new BizException("超级管理员账号不允许删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    /**
     * 批量删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        ids.forEach(this::delete);
    }

    /**
     * 重置密码
     */
    public String resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        String pwd = (newPassword == null || newPassword.isBlank()) ? DEFAULT_PASSWORD : newPassword;
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getPassword, passwordEncoder.encode(pwd)));
        return pwd;
    }

    /**
     * 启用/停用
     */
    public void changeStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        if ("admin".equals(user.getUsername()) && status != null && status == 0) {
            throw new BizException("超级管理员账号不允许停用");
        }
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getStatus, status));
    }

    /**
     * 分配角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        saveRoles(userId, roleIds);
    }

    /**
     * 供其它模块创建账号使用
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUser createAccount(String username, String realName, String userType,
                                 Integer gender, String phone, String email, Long roleId) {
        checkUsernameUnique(username, null);
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setRealName(realName);
        user.setUserType(userType);
        user.setGender(gender);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(1);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userMapper.insert(user);
        if (roleId != null) {
            userRoleMapper.insert(new SysUserRole(null, user.getId(), roleId));
        }
        return user;
    }

    /** 更新账号基础信息 */
    public void updateAccount(Long userId, String realName, Integer gender, String phone, String email) {
        if (userId == null) {
            return;
        }
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(realName != null, SysUser::getRealName, realName)
                .set(gender != null, SysUser::getGender, gender)
                .set(phone != null, SysUser::getPhone, phone)
                .set(email != null, SysUser::getEmail, email));
    }

    /** 删除账号 */
    public void deleteAccount(Long userId) {
        if (userId == null) {
            return;
        }
        userMapper.deleteById(userId);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
    }

    public SysUser getByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username).last("LIMIT 1"));
    }

    // ==================== 私有方法 ====================

    private void saveRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        roleIds.stream().distinct().forEach(roleId ->
                userRoleMapper.insert(new SysUserRole(null, userId, roleId)));
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        if (username == null || username.isBlank()) {
            throw new BizException("登录账号不能为空");
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("登录账号【" + username + "】已存在");
        }
    }

    /** 批量填充角色信息 */
    private void fillRoles(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> ids = users.stream().map(SysUser::getId).collect(Collectors.toList());
        List<UserRoleDTO> rows = userMapper.selectRoleRowsByUserIds(ids);
        Map<Long, List<UserRoleDTO>> grouped = rows.stream()
                .collect(Collectors.groupingBy(UserRoleDTO::getUserId, LinkedHashMap::new, Collectors.toList()));
        for (SysUser user : users) {
            List<UserRoleDTO> roles = grouped.getOrDefault(user.getId(), List.of());
            user.setRoleIds(roles.stream().map(UserRoleDTO::getRoleId).collect(Collectors.toList()));
            user.setRoleNames(roles.stream().map(UserRoleDTO::getRoleName).collect(Collectors.toList()));
            user.setRoleCodes(roles.stream().map(UserRoleDTO::getRoleCode).collect(Collectors.toList()));
        }
    }
}
