package com.aas.mapper;

import com.aas.dto.query.TeacherQuery;
import com.aas.entity.TeaTeacher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeaTeacherMapper extends BaseMapper<TeaTeacher> {

    /** 分页查询教师 */
    IPage<TeaTeacher> selectTeacherPage(IPage<TeaTeacher> page, @Param("q") TeacherQuery query);

    /** 教师详情(含所带班级) */
    TeaTeacher selectTeacherDetail(@Param("id") Long id);

    /** 按用户ID查询教师 */
    TeaTeacher selectByUserId(@Param("userId") Long userId);

    /** 查询班主任所管理的班级ID */
    List<Long> selectManageClassIds(@Param("teacherId") Long teacherId);

    /** 查询班主任所管理的班级名称 */
    List<String> selectManageClassNames(@Param("teacherId") Long teacherId);
}
