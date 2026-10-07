package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.DataScope;
import com.aas.dto.query.LeaveQuery;
import com.aas.entity.AttLeave;
import com.aas.security.SecurityUtils;
import com.aas.service.AttLeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 请假管理
 */
@Tag(name = "18-请假管理")
@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class AttLeaveController {

    private final AttLeaveService leaveService;

    @Operation(summary = "分页查询请假申请(自动应用数据权限)")
    @PreAuthorize("@ss.hasAnyPerm('leave:approve','leave:my')")
    @GetMapping("/page")
    public Result<PageResult<AttLeave>> page(LeaveQuery query) {
        return Result.success(leaveService.page(query, DataScope.of(SecurityUtils.getLoginUser())));
    }

    @Operation(summary = "请假详情")
    @PreAuthorize("@ss.hasAnyPerm('leave:approve','leave:my')")
    @GetMapping("/{id}")
    public Result<AttLeave> detail(@PathVariable Long id) {
        return Result.success(leaveService.detail(id));
    }

    @Operation(summary = "提交请假申请")
    @OperLog(module = "请假管理", operation = "提交请假申请")
    @PreAuthorize("@ss.hasPerm('leave:my')")
    @PostMapping
    public Result<Long> apply(@RequestBody AttLeave leave) {
        return Result.success("申请已提交，等待班主任审批", leaveService.apply(leave));
    }

    @Operation(summary = "审批请假")
    @OperLog(module = "请假管理", operation = "审批请假")
    @PreAuthorize("@ss.hasPerm('leave:approve')")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam Integer status,
                                @RequestParam(required = false) String remark) {
        leaveService.approve(id, status, remark);
        return Result.success(status != null && status == 1 ? "已通过" : "已驳回", null);
    }

    @Operation(summary = "批量审批")
    @OperLog(module = "请假管理", operation = "批量审批请假")
    @PreAuthorize("@ss.hasPerm('leave:approve')")
    @PutMapping("/batch-approve")
    public Result<Integer> batchApprove(@RequestBody List<Long> ids,
                                        @RequestParam Integer status,
                                        @RequestParam(required = false) String remark) {
        return Result.success("审批完成", leaveService.batchApprove(ids, status, remark));
    }

    @Operation(summary = "撤销我的申请")
    @OperLog(module = "请假管理", operation = "撤销请假申请")
    @PreAuthorize("@ss.hasPerm('leave:my')")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        leaveService.cancel(id);
        return Result.success("已撤销", null);
    }

    @Operation(summary = "待审批数量")
    @PreAuthorize("@ss.hasAnyPerm('leave:approve','leave:my')")
    @GetMapping("/pending-count")
    public Result<Map<String, Object>> pendingCount() {
        DataScope scope = DataScope.of(SecurityUtils.getLoginUser());
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("pending", leaveService.pendingCount(scope));
        map.put("myPending", SecurityUtils.getStudentId() == null ? 0
                : leaveService.myPendingCount(SecurityUtils.getStudentId()));
        return Result.success(map);
    }

    @Operation(summary = "删除请假记录")
    @OperLog(module = "请假管理", operation = "删除请假记录")
    @PreAuthorize("@ss.hasPerm('leave:approve')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        leaveService.delete(id);
        return Result.success("删除成功", null);
    }
}
