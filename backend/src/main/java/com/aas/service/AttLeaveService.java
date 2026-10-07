package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.exception.BizException;
import com.aas.dto.DataScope;
import com.aas.dto.query.LeaveQuery;
import com.aas.entity.AttLeave;
import com.aas.entity.StuStudent;
import com.aas.mapper.AttLeaveMapper;
import com.aas.mapper.StuStudentMapper;
import com.aas.security.LoginUser;
import com.aas.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假服务
 */
@Service
@RequiredArgsConstructor
public class AttLeaveService {

    private final AttLeaveMapper leaveMapper;
    private final StuStudentMapper studentMapper;

    public PageResult<AttLeave> page(LeaveQuery query, DataScope scope) {
        Page<AttLeave> page = query.toPage();
        return PageResult.of(leaveMapper.selectLeavePage(page, query, scope));
    }

    public AttLeave detail(Long id) {
        AttLeave leave = leaveMapper.selectLeaveDetail(id);
        if (leave == null) {
            throw new BizException("请假记录不存在");
        }
        return leave;
    }

    /**
     * 学生提交请假申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Long apply(AttLeave leave) {
        LoginUser user = SecurityUtils.getLoginUser();
        Long studentId = leave.getStudentId() != null ? leave.getStudentId() : user.getStudentId();
        if (studentId == null) {
            throw new BizException("未找到学生档案，无法提交申请");
        }
        if (leave.getStartDate() == null || leave.getEndDate() == null) {
            throw new BizException("请填写请假起止时间");
        }
        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new BizException("结束时间不能早于开始时间");
        }
        if (leave.getReason() == null || leave.getReason().isBlank()) {
            throw new BizException("请填写请假事由");
        }
        // 计算天数
        long minutes = Duration.between(leave.getStartDate(), leave.getEndDate()).toMinutes();
        double days = Math.max(0.5, Math.round(minutes / 60.0 / 24.0 * 2) / 2.0);
        leave.setDays(BigDecimal.valueOf(days));
        leave.setStudentId(studentId);
        leave.setStatus(0);
        leave.setCreateTime(LocalDateTime.now());
        leave.setApproverId(null);
        leave.setApproverName(null);
        leave.setApproveTime(null);
        leave.setApproveRemark(null);
        leaveMapper.insert(leave);
        return leave.getId();
    }

    /**
     * 审批请假
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Integer status, String remark) {
        if (status == null || (status != 1 && status != 2)) {
            throw new BizException("审批结果不正确");
        }
        AttLeave leave = leaveMapper.selectLeaveDetail(id);
        if (leave == null) {
            throw new BizException("请假记录不存在");
        }
        if (leave.getStatus() != null && leave.getStatus() != 0) {
            throw new BizException("该申请已被处理，无法重复审批");
        }
        checkApprovePermission(leave);
        LoginUser user = SecurityUtils.getLoginUser();
        leaveMapper.update(null, new LambdaUpdateWrapper<AttLeave>()
                .eq(AttLeave::getId, id)
                .set(AttLeave::getStatus, status)
                .set(AttLeave::getApproverId, user.getUserId())
                .set(AttLeave::getApproverName, user.getRealName())
                .set(AttLeave::getApproveTime, LocalDateTime.now())
                .set(AttLeave::getApproveRemark, remark));
    }

    /**
     * 批量审批
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchApprove(List<Long> ids, Integer status, String remark) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要审批的申请");
        }
        int count = 0;
        for (Long id : ids) {
            approve(id, status, remark);
            count++;
        }
        return count;
    }

    /**
     * 学生撤销申请
     */
    public void cancel(Long id) {
        AttLeave leave = leaveMapper.selectById(id);
        if (leave == null) {
            throw new BizException("请假记录不存在");
        }
        LoginUser user = SecurityUtils.getLoginUser();
        if (leave.getStudentId() == null || !leave.getStudentId().equals(user.getStudentId())) {
            throw new BizException("只能撤销本人的请假申请");
        }
        if (leave.getStatus() != null && leave.getStatus() != 0) {
            throw new BizException("已处理的申请不可撤销");
        }
        leaveMapper.update(null, new LambdaUpdateWrapper<AttLeave>()
                .eq(AttLeave::getId, id)
                .set(AttLeave::getStatus, 3));
    }

    public void delete(Long id) {
        leaveMapper.deleteById(id);
    }

    /** 待审批数量 */
    public Integer pendingCount(DataScope scope) {
        return leaveMapper.countPending(scope);
    }

    /**
     * 审批权限：班主任仅限本班；教务处/管理员不限
     */
    private void checkApprovePermission(AttLeave leave) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user.isAdmin() || user.isAcademic()) {
            return;
        }
        StuStudent student = studentMapper.selectById(leave.getStudentId());
        if (student == null) {
            throw new BizException("学生档案不存在");
        }
        if (user.getManageClassIds() == null || !user.getManageClassIds().contains(student.getClassId())) {
            throw new BizException("您只能审批本班学生的请假申请");
        }
    }

    /** 学生待审批数量 */
    public long myPendingCount(Long studentId) {
        return leaveMapper.selectCount(new LambdaQueryWrapper<AttLeave>()
                .eq(AttLeave::getStudentId, studentId)
                .eq(AttLeave::getStatus, 0));
    }
}
