package com.aas.mapper;

import com.aas.dto.DataScope;
import com.aas.dto.query.StudentQuery;
import com.aas.entity.StuStudent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StuStudentMapper extends BaseMapper<StuStudent> {

    /** 分页查询学生(带院系/专业/班级名称，含数据权限) */
    IPage<StuStudent> selectStudentPage(IPage<StuStudent> page,
                                        @Param("q") StudentQuery query,
                                        @Param("scope") DataScope scope);

    /** 学生详情 */
    StuStudent selectStudentDetail(@Param("id") Long id);

    /** 按用户ID查询学生 */
    StuStudent selectByUserId(@Param("userId") Long userId);
}
