package com.aas.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 权限校验服务，供 @PreAuthorize("@ss.hasPerm('xxx')") 使用
 */
@Component("ss")
public class SecurityService {

    /** 是否拥有指定权限 */
    public boolean hasPerm(String permission) {
        if (permission == null || permission.isBlank()) {
            return true;
        }
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(permission::equals);
    }

    /** 是否拥有其中任意一个权限 */
    public boolean hasAnyPerm(String... permissions) {
        return Arrays.stream(permissions).anyMatch(this::hasPerm);
    }

    /** 是否拥有指定角色 */
    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return true;
        }
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(target::equals);
    }

    /** 是否拥有其中任意一个角色 */
    public boolean hasAnyRole(String... roles) {
        return Arrays.stream(roles).anyMatch(this::hasRole);
    }
}
