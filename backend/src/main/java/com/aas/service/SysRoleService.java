package com.aas.service;

import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.entity.SysRole;
import com.aas.entity.SysRoleMenu;
import com.aas.mapper.SysMenuMapper;
import com.aas.mapper.SysRoleMapper;
import com.aas.mapper.SysRoleMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务
 */
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    public List<SysRole> list(String keyword, Integer status) {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .like(keyword != null && !keyword.isBlank(), SysRole::getRoleName, keyword)
                .eq(status != null, SysRole::getStatus, status)
                .orderByAsc(SysRole::getSort));
    }

    public SysRole detail(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        return role;
    }

    /** 角色拥有的菜单ID */
    public List<Long> menuIds(Long roleId) {
        return menuMapper.selectMenuIdsByRoleId(roleId);
    }

    public Long create(SysRole role, List<Long> menuIds) {
        checkCodeUnique(role.getRoleCode(), null);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        if (role.getDataScope() == null) {
            role.setDataScope("SELF");
        }
        roleMapper.insert(role);
        saveMenus(role.getId(), menuIds);
        return role.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysRole role, List<Long> menuIds) {
        SysRole exists = roleMapper.selectById(role.getId());
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        checkCodeUnique(role.getRoleCode(), role.getId());
        roleMapper.updateById(role);
        if (menuIds != null) {
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, role.getId()));
            saveMenus(role.getId(), menuIds);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        if (List.of("ADMIN", "ACADEMIC", "HEAD_TEACHER", "STUDENT").contains(role.getRoleCode())) {
            throw new BizException("系统内置角色不允许删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        saveMenus(roleId, menuIds);
    }

    public void changeStatus(Long id, Integer status) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        if ("ADMIN".equals(role.getRoleCode()) && status != null && status == 0) {
            throw new BizException("系统管理员角色不允许停用");
        }
        SysRole update = new SysRole();
        update.setId(id);
        update.setStatus(status);
        roleMapper.updateById(update);
    }

    private void saveMenus(Long roleId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        menuIds.stream().distinct().forEach(menuId ->
                roleMenuMapper.insert(new SysRoleMenu(null, roleId, menuId)));
    }

    private void checkCodeUnique(String roleCode, Long excludeId) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new BizException("角色标识不能为空");
        }
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .ne(excludeId != null, SysRole::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("角色标识【" + roleCode + "】已存在");
        }
    }
}
