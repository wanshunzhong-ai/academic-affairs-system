package com.aas.vo;

import com.aas.entity.SysMenu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Schema(description = "登录用户信息")
public class UserInfoVO {

    private Long userId;

    private String username;

    private String realName;

    private String userType;

    private Integer gender;

    private String avatar;

    private String phone;

    private String email;

    @Schema(description = "主角色标识 STUDENT/HEAD_TEACHER/ACADEMIC/ADMIN")
    private String roleCode;

    @Schema(description = "主角色名称")
    private String roleName;

    @Schema(description = "全部角色标识")
    private Set<String> roles;

    @Schema(description = "权限标识集合")
    private Set<String> permissions;

    @Schema(description = "数据范围 SELF/CLASS/ALL")
    private String dataScope;

    // ============ 业务档案 ============

    @Schema(description = "学生档案ID")
    private Long studentId;

    @Schema(description = "教师档案ID")
    private Long teacherId;

    @Schema(description = "学生所属班级ID")
    private Long classId;

    @Schema(description = "学生所属班级名称")
    private String className;

    @Schema(description = "学生学号")
    private String studentNo;

    @Schema(description = "教师工号")
    private String teacherNo;

    @Schema(description = "班主任管理的班级ID")
    private List<Long> manageClassIds = new ArrayList<>();

    @Schema(description = "班主任管理的班级名称")
    private List<String> manageClassNames = new ArrayList<>();

    @Schema(description = "所属院系名称")
    private String deptName;

    @Schema(description = "所授课程数(教师)")
    private Integer teachingCount;

    @Schema(description = "菜单树")
    private List<SysMenu> menus = new ArrayList<>();
}
