package com.aas.security;

import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户，未登录抛出异常
     */
    public static LoginUser getLoginUser() {
        LoginUser loginUser = getLoginUserOrNull();
        if (loginUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    /**
     * 获取当前登录用户，未登录返回 null
     */
    public static LoginUser getLoginUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }

    /** 当前用户ID */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /** 当前用户ID，未登录返回 null */
    public static Long getUserIdOrNull() {
        LoginUser user = getLoginUserOrNull();
        return user == null ? null : user.getUserId();
    }

    /** 当前用户姓名 */
    public static String getRealName() {
        return getLoginUser().getRealName();
    }

    /** 当前学生档案ID */
    public static Long getStudentId() {
        return getLoginUser().getStudentId();
    }

    /** 当前教师档案ID */
    public static Long getTeacherId() {
        return getLoginUser().getTeacherId();
    }

    /** 是否管理员 */
    public static boolean isAdmin() {
        LoginUser user = getLoginUserOrNull();
        return user != null && user.isAdmin();
    }

    /** 是否教务处 */
    public static boolean isAcademic() {
        LoginUser user = getLoginUserOrNull();
        return user != null && user.isAcademic();
    }

    /** 是否班主任 */
    public static boolean isHeadTeacher() {
        LoginUser user = getLoginUserOrNull();
        return user != null && user.isHeadTeacher();
    }

    /** 是否学生 */
    public static boolean isStudent() {
        LoginUser user = getLoginUserOrNull();
        return user != null && user.isStudent();
    }

    /**
     * 是否为全局数据权限(管理员/教务处，可见全部数据)
     */
    public static boolean isGlobalScope() {
        LoginUser user = getLoginUserOrNull();
        if (user == null) {
            return false;
        }
        return user.isAdmin() || user.isAcademic() || "ALL".equals(user.getDataScope());
    }
}
