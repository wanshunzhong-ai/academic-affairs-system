package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.query.NoticeQuery;
import com.aas.entity.SysNotice;
import com.aas.security.SecurityUtils;
import com.aas.service.SysNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知公告
 */
@Tag(name = "19-通知公告")
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class SysNoticeController {

    private final SysNoticeService noticeService;

    @Operation(summary = "我可见的公告列表")
    @PreAuthorize("@ss.hasPerm('notice:list')")
    @GetMapping("/page")
    public Result<PageResult<SysNotice>> page(NoticeQuery query) {
        return Result.success(noticeService.visiblePage(query, SecurityUtils.getLoginUser()));
    }

    @Operation(summary = "公告管理列表(发布者视角)")
    @PreAuthorize("@ss.hasPerm('notice:manage')")
    @GetMapping("/manage/page")
    public Result<PageResult<SysNotice>> managePage(NoticeQuery query) {
        return Result.success(noticeService.page(query));
    }

    @Operation(summary = "公告详情")
    @PreAuthorize("@ss.hasPerm('notice:list')")
    @GetMapping("/{id}")
    public Result<SysNotice> detail(@PathVariable Long id) {
        return Result.success(noticeService.detail(id));
    }

    @Operation(summary = "标记为已读")
    @PreAuthorize("@ss.hasPerm('notice:list')")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        noticeService.markRead(id, SecurityUtils.getUserId());
        return Result.success("已标记为已读", null);
    }

    @Operation(summary = "未读公告数量")
    @PreAuthorize("@ss.hasPerm('notice:list')")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(noticeService.unreadCount(SecurityUtils.getLoginUser()));
    }

    @Operation(summary = "发布公告")
    @OperLog(module = "通知公告", operation = "发布公告")
    @PreAuthorize("@ss.hasPerm('notice:manage')")
    @PostMapping
    public Result<Long> create(@RequestBody SysNotice notice) {
        return Result.success("发布成功", noticeService.create(notice, SecurityUtils.getLoginUser()));
    }

    @Operation(summary = "修改公告")
    @OperLog(module = "通知公告", operation = "修改公告")
    @PreAuthorize("@ss.hasPerm('notice:manage')")
    @PutMapping
    public Result<Void> update(@RequestBody SysNotice notice) {
        noticeService.update(notice, SecurityUtils.getLoginUser());
        return Result.success("修改成功", null);
    }

    @Operation(summary = "发布/下架公告")
    @OperLog(module = "通知公告", operation = "修改公告状态")
    @PreAuthorize("@ss.hasPerm('notice:manage')")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        noticeService.changeStatus(id, status, SecurityUtils.getLoginUser());
        return Result.success("操作成功", null);
    }

    @Operation(summary = "删除公告")
    @OperLog(module = "通知公告", operation = "删除公告")
    @PreAuthorize("@ss.hasPerm('notice:manage')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id, SecurityUtils.getLoginUser());
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量删除公告")
    @OperLog(module = "通知公告", operation = "批量删除公告")
    @PreAuthorize("@ss.hasPerm('notice:manage')")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        noticeService.deleteBatch(ids, SecurityUtils.getLoginUser());
        return Result.success("删除成功", null);
    }
}
