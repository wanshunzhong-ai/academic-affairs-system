package com.aas.mapper;

import com.aas.entity.CourseSchedule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseScheduleMapper extends BaseMapper<CourseSchedule> {

    /** 按开课ID查询排课(含教室) */
    List<CourseSchedule> selectByOfferingId(@Param("offeringId") Long offeringId);

    /** 批量按开课ID查询排课 */
    List<CourseSchedule> selectByOfferingIds(@Param("offeringIds") List<Long> offeringIds);

    /** 查询排课冲突（教师/班级/教室） */
    List<java.util.Map<String, Object>> selectConflicts(@Param("offeringId") Long offeringId,
                                                        @Param("semesterId") Long semesterId,
                                                        @Param("weekDay") Integer weekDay,
                                                        @Param("startSection") Integer startSection,
                                                        @Param("endSection") Integer endSection,
                                                        @Param("startWeek") Integer startWeek,
                                                        @Param("endWeek") Integer endWeek,
                                                        @Param("teacherId") Long teacherId,
                                                        @Param("classId") Long classId,
                                                        @Param("classroomId") Long classroomId);

    /** 删除某开课的全部排课 */
    int deleteByOfferingId(@Param("offeringId") Long offeringId);
}
