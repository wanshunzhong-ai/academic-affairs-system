package com.aas.mapper;

import com.aas.dto.query.OfferingQuery;
import com.aas.entity.CourseOffering;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseOfferingMapper extends BaseMapper<CourseOffering> {

    /** 分页查询开课安排 */
    IPage<CourseOffering> selectOfferingPage(IPage<CourseOffering> page, @Param("q") OfferingQuery query);

    /** 开课详情 */
    CourseOffering selectOfferingDetail(@Param("id") Long id);

    /** 学生课表 */
    List<CourseOffering> selectStudentTimetable(@Param("studentId") Long studentId,
                                                @Param("semesterId") Long semesterId);

    /** 教师课表 */
    List<CourseOffering> selectTeacherTimetable(@Param("teacherId") Long teacherId,
                                                @Param("semesterId") Long semesterId);

    /** 班级课表 */
    List<CourseOffering> selectClassTimetable(@Param("classId") Long classId,
                                              @Param("semesterId") Long semesterId);

    /** 学生可选课程(公选课 + 本班未选课程) */
    IPage<CourseOffering> selectSelectableOfferings(IPage<CourseOffering> page,
                                                    @Param("studentId") Long studentId,
                                                    @Param("classId") Long classId,
                                                    @Param("semesterId") Long semesterId,
                                                    @Param("keyword") String keyword);

    /** 某课程的已选学生数 */
    Integer countSelected(@Param("offeringId") Long offeringId);
}
