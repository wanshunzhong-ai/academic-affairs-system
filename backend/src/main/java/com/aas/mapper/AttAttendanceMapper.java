package com.aas.mapper;

import com.aas.dto.DataScope;
import com.aas.dto.query.AttendanceQuery;
import com.aas.entity.AttAttendance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AttAttendanceMapper extends BaseMapper<AttAttendance> {

    /** 分页查询考勤 */
    IPage<AttAttendance> selectAttendancePage(IPage<AttAttendance> page,
                                              @Param("q") AttendanceQuery query,
                                              @Param("scope") DataScope scope);

    /** 学生考勤统计 */
    List<Map<String, Object>> selectStudentStats(@Param("studentId") Long studentId);

    /** 班级考勤统计 */
    List<Map<String, Object>> selectClassStats(@Param("classId") Long classId);
}
