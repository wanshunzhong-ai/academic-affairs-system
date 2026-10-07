package com.aas.mapper;

import com.aas.dto.DataScope;
import com.aas.dto.query.LeaveQuery;
import com.aas.entity.AttLeave;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AttLeaveMapper extends BaseMapper<AttLeave> {

    /** 分页查询请假申请 */
    IPage<AttLeave> selectLeavePage(IPage<AttLeave> page,
                                    @Param("q") LeaveQuery query,
                                    @Param("scope") DataScope scope);

    /** 请假详情 */
    AttLeave selectLeaveDetail(@Param("id") Long id);

    /** 待审批数量 */
    Integer countPending(@Param("scope") DataScope scope);
}
