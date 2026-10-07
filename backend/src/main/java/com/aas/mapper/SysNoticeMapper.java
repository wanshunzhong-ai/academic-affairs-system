package com.aas.mapper;

import com.aas.dto.query.NoticeQuery;
import com.aas.entity.SysNotice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysNoticeMapper extends BaseMapper<SysNotice> {

    /** 管理端分页查询公告 */
    IPage<SysNotice> selectNoticePage(IPage<SysNotice> page, @Param("q") NoticeQuery query);

    /** 用户可见的公告列表 */
    IPage<SysNotice> selectVisibleNoticePage(IPage<SysNotice> page,
                                             @Param("q") NoticeQuery query,
                                             @Param("userId") Long userId,
                                             @Param("roleCode") String roleCode,
                                             @Param("classId") Long classId);

    /** 公告详情 */
    SysNotice selectNoticeDetail(@Param("id") Long id);
}
