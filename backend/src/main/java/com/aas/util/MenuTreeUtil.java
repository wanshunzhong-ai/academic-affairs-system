package com.aas.util;

import com.aas.entity.SysMenu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单树构建工具
 */
public final class MenuTreeUtil {

    private MenuTreeUtil() {
    }

    /**
     * 构建树形结构
     */
    public static List<SysMenu> build(List<SysMenu> menus, Long rootParentId) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, SysMenu> map = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            menu.setChildren(new ArrayList<>());
            map.put(menu.getId(), menu);
        }
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            Long pid = menu.getParentId() == null ? rootParentId : menu.getParentId();
            if (pid.equals(rootParentId) || !map.containsKey(pid)) {
                roots.add(menu);
            } else {
                map.get(pid).getChildren().add(menu);
            }
        }
        return roots;
    }
}
