package com.aas.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 登录用户上下文
 */
@Data
@NoArgsConstructor
public class LoginUser implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    private String username;

    @JsonIgnore
    private String password;

    private String realName;

    /** 用户类型: STUDENT/TEACHER/ADMIN */
    private String userType;

    private Integer gender;

    private String avatar;

    private String phone;

    private String email;

    /** 状态 0停用 1正常 */
    private Integer status;

    /** 数据范围: SELF / CLASS / ALL */
    private String dataScope = "SELF";

    /** 主角色标识 */
    private String roleCode;

    /** 主角色名称 */
    private String roleName;

    /** 角色标识集合 */
    private Set<String> roles = new HashSet<>();

    /** 权限标识集合 */
    private Set<String> permissions = new HashSet<>();

    /** 学生档案ID(学生身份) */
    private Long studentId;

    /** 教师档案ID(教师身份) */
    private Long teacherId;

    /** 所属班级ID(学生身份) */
    private Long classId;

    /** 所管理的班级ID列表(班主任身份) */
    private List<Long> manageClassIds = new ArrayList<>();

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (roles != null) {
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        }
        if (permissions != null) {
            permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }
        return authorities;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return status != null && status == 1;
    }

    /** 是否管理员 */
    @JsonIgnore
    public boolean isAdmin() {
        return roles != null && roles.contains("ADMIN");
    }

    /** 是否教务处 */
    @JsonIgnore
    public boolean isAcademic() {
        return roles != null && roles.contains("ACADEMIC");
    }

    /** 是否班主任 */
    @JsonIgnore
    public boolean isHeadTeacher() {
        return roles != null && roles.contains("HEAD_TEACHER");
    }

    /** 是否学生 */
    @JsonIgnore
    public boolean isStudent() {
        return roles != null && roles.contains("STUDENT");
    }
}
