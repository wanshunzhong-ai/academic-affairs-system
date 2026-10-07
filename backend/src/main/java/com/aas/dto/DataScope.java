package com.aas.dto;

import com.aas.security.LoginUser;
import lombok.Data;

import java.util.List;

/**
 * 数据权限范围（由当前登录用户推导，防止越权查询）
 */
@Data
public class DataScope {

    /** true 表示可查看全部数据 */
    private boolean global = true;

    /** 学生的班级ID */
    private Long classId;

    /** 所管理的班级ID列表(班主任) */
    private List<Long> classIds;

    /** 学生档案ID */
    private Long studentId;

    /** 教师档案ID */
    private Long teacherId;

    public boolean isRestricted() {
        return !global;
    }

    /**
     * 根据登录用户构造数据权限范围
     */
    public static DataScope of(LoginUser user) {
        DataScope scope = new DataScope();
        if (user == null) {
            scope.setGlobal(false);
            return scope;
        }
        if (user.isAdmin() || user.isAcademic()) {
            scope.setGlobal(true);
            return scope;
        }
        scope.setGlobal(false);
        scope.setStudentId(user.getStudentId());
        scope.setTeacherId(user.getTeacherId());
        scope.setClassId(user.getClassId());
        if (user.isHeadTeacher()) {
            scope.setClassIds(user.getManageClassIds());
        }
        return scope;
    }
}
