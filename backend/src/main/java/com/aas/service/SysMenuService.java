package com.aas.service;

import com.aas.common.exception.BizException;
import com.aas.entity.SysMenu;
import com.aas.mapper.SysMenuMapper;
import com.aas.util.MenuTreeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜单服务
 */
@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper menuMapper;

    /** 菜单树(全部) */
    public List<SysMenu> tree(String keyword) {
        List<SysMenu> menus = menuMapper.selectAllMenus();
        if (keyword != null && !keyword.isBlank()) {
            menus = menus.stream()
                    .filter(m -> m.getMenuName() != null && m.getMenuName().contains(keyword))
                    .toList();
        }
        return MenuTreeUtil.build(menus, 0L);
    }

    public List<SysMenu> list() {
        return menuMapper.selectAllMenus();
    }

    public Long create(SysMenu menu) {
        validate(menu);
        menuMapper.insert(menu);
        return menu.getId();
    }

    public void update(SysMenu menu) {
        validate(menu);
        if (menu.getId() != null && menu.getId().equals(menu.getParentId())) {
            throw new BizException("上级菜单不能是自己");
        }
        menuMapper.updateById(menu);
    }

    public void delete(Long id) {
        Long children = menuMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (children != null && children > 0) {
            throw new BizException("存在子菜单，请先删除子菜单");
        }
        menuMapper.deleteById(id);
    }

    private void validate(SysMenu menu) {
        if (menu.getMenuName() == null || menu.getMenuName().isBlank()) {
            throw new BizException("菜单名称不能为空");
        }
        if (menu.getMenuType() == null || menu.getMenuType().isBlank()) {
            throw new BizException("菜单类型不能为空");
        }
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getSort() == null) {
            menu.setSort(0);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
    }
}
