package com.aas.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户-角色扁平行
 */
@Data
public class UserRoleDTO implements Serializable {

    private Long userId;

    private Long roleId;

    private String roleName;

    private String roleCode;
}
