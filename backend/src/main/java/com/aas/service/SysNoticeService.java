package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.exception.BizException;
import com.aas.dto.PageQuery;
import com.aas.dto.query.NoticeQuery;
import com.aas.entity.SysNotice;
import com.aas.entity.SysNoticeRead;
import com.aas.mapper.SysNoticeMapper;
import com.aas.mapper.SysNoticeReadMapper;
import com.aas.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 通知公告服务
 */
@Service
@RequiredArgsConstructor
public class SysNoticeService {

    private final SysNoticeMapper noticeMapper;
    private final SysNoticeReadMapper noticeReadMapper;

    /** 管理端分页 */
    public PageResult<SysNotice> page(NoticeQuery query) {
        Page<SysNotice> page = query.toPage();
        return PageResult.of(noticeMapper.selectNoticePage(page, query));
    }

    /** 用户可见公告列表 */
    public PageResult<SysNotice> visiblePage(NoticeQuery query, LoginUser user) {
        Page<SysNotice> page = query.toPage();
        return PageResult.of(noticeMapper.selectVisibleNoticePage(page, query,
                user.getUserId(), user.getRoleCode(), user.getClassId()));
    }

    public SysNotice detail(Long id) {
        SysNotice notice = noticeMapper.selectNoticeDetail(id);
        if (notice == null) {
            throw new BizException("公告不存在或已被删除");
        }
        return notice;
    }

    /** 阅读公告 */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long noticeId, Long userId) {
        Long exists = noticeReadMapper.selectCount(new LambdaQueryWrapper<SysNoticeRead>()
                .eq(SysNoticeRead::getNoticeId, noticeId)
                .eq(SysNoticeRead::getUserId, userId));
        if (exists != null && exists > 0) {
            return;
        }
        SysNoticeRead read = new SysNoticeRead();
        read.setNoticeId(noticeId);
        read.setUserId(userId);
        read.setReadTime(LocalDateTime.now());
        noticeReadMapper.insert(read);
    }

    /** 未读数量 */
    public long unreadCount(LoginUser user) {
        NoticeQuery query = new NoticeQuery();
        query.setPageNum(1);
        query.setPageSize(500);
        var result = noticeMapper.selectVisibleNoticePage(new Page<>(1, 500), query,
                user.getUserId(), user.getRoleCode(), user.getClassId());
        return result.getRecords().stream()
                .filter(n -> n.getReadFlag() == null || !n.getReadFlag())
                .count();
    }

    /**
     * 发布公告
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysNotice notice, LoginUser user) {
        validate(notice);
        notice.setPublisherId(user.getUserId());
        notice.setPublisherName(user.getRealName());
        notice.setPublisherRole(user.getRoleName() == null ? "教师" : user.getRoleName());
        if (notice.getStatus() == null) {
            notice.setStatus(1);
        }
        if (notice.getStatus() == 1) {
            notice.setPublishTime(LocalDateTime.now());
        }
        // 班主任只能发布本班公告
        if (user.isHeadTeacher()) {
            if (!"CLASS".equals(notice.getScope())) {
                throw new BizException("班主任仅可发布本班公告");
            }
            if (notice.getClassId() == null
                    || user.getManageClassIds() == null
                    || !user.getManageClassIds().contains(notice.getClassId())) {
                throw new BizException("您只能向本人所带班级发布公告");
            }
        }
        noticeMapper.insert(notice);
        return notice.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysNotice notice, LoginUser user) {
        SysNotice exists = noticeMapper.selectById(notice.getId());
        if (exists == null) {
            throw new BizException("公告不存在");
        }
        checkModifyPermission(exists, user);
        validate(notice);
        if (notice.getStatus() != null && notice.getStatus() == 1 && exists.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        noticeMapper.updateById(notice);
    }

    /** 发布/下架 */
    public void changeStatus(Long id, Integer status, LoginUser user) {
        SysNotice exists = noticeMapper.selectById(id);
        if (exists == null) {
            throw new BizException("公告不存在");
        }
        checkModifyPermission(exists, user);
        noticeMapper.update(null, new LambdaUpdateWrapper<SysNotice>()
                .eq(SysNotice::getId, id)
                .set(SysNotice::getStatus, status)
                .set(status != null && status == 1, SysNotice::getPublishTime, LocalDateTime.now()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, LoginUser user) {
        SysNotice exists = noticeMapper.selectById(id);
        if (exists == null) {
            throw new BizException("公告不存在");
        }
        checkModifyPermission(exists, user);
        noticeMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(java.util.List<Long> ids, LoginUser user) {
        ids.forEach(id -> delete(id, user));
    }

    private void checkModifyPermission(SysNotice notice, LoginUser user) {
        if (user.isAdmin() || user.isAcademic()) {
            return;
        }
        if (user.isHeadTeacher()) {
            if (!"CLASS".equals(notice.getScope())
                    || user.getManageClassIds() == null
                    || !user.getManageClassIds().contains(notice.getClassId())) {
                throw new BizException("您只能管理本人发布的本班公告");
            }
            return;
        }
        throw new BizException("您没有管理公告的权限");
    }

    private void validate(SysNotice notice) {
        if (notice.getTitle() == null || notice.getTitle().isBlank()) {
            throw new BizException("公告标题不能为空");
        }
        if (notice.getContent() == null || notice.getContent().isBlank()) {
            throw new BizException("公告内容不能为空");
        }
        if (notice.getScope() == null) {
            notice.setScope("ALL");
        }
        if ("CLASS".equals(notice.getScope()) && notice.getClassId() == null) {
            throw new BizException("请选择目标班级");
        }
        if ("ROLE".equals(notice.getScope()) && (notice.getTargetRole() == null || notice.getTargetRole().isBlank())) {
            throw new BizException("请选择目标角色");
        }
        if (notice.getNoticeType() == null) {
            notice.setNoticeType("通知");
        }
    }

    /** 通用分页参数(预留给扩展) */
    public PageQuery pageQuery() {
        return new PageQuery();
    }
}
