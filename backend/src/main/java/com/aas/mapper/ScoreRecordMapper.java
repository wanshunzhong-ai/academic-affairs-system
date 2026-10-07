package com.aas.mapper;

import com.aas.dto.DataScope;
import com.aas.dto.query.ScoreQuery;
import com.aas.entity.ScoreRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ScoreRecordMapper extends BaseMapper<ScoreRecord> {

    /** 分页查询成绩 */
    IPage<ScoreRecord> selectScorePage(IPage<ScoreRecord> page,
                                       @Param("q") ScoreQuery query,
                                       @Param("scope") DataScope scope);

    /** 学生成绩单 */
    List<ScoreRecord> selectByStudent(@Param("studentId") Long studentId,
                                      @Param("semesterId") Long semesterId);

    /** 某开课的全部成绩 */
    List<ScoreRecord> selectByOffering(@Param("offeringId") Long offeringId);

    /** 课程成绩分布统计 */
    List<Map<String, Object>> selectScoreDistribution(@Param("offeringId") Long offeringId);

    /** 班级/课程平均分统计 */
    List<Map<String, Object>> selectCourseAvgStats(@Param("classId") Long classId,
                                                   @Param("semesterId") Long semesterId);

    /** 学生总览统计 */
    Map<String, Object> selectStudentSummary(@Param("studentId") Long studentId);

    /** 某开课成绩概览 */
    Map<String, Object> selectOfferingSummary(@Param("offeringId") Long offeringId);

    /** 全局成绩等级分布 */
    List<Map<String, Object>> selectSelectDistributionGlobal();

    /** 某学生成绩等级分布 */
    List<Map<String, Object>> selectStudentScoreDistribution(@Param("studentId") Long studentId);

    /** 某班学生成绩排名(前N名) */
    List<Map<String, Object>> selectClassStudentRank(@Param("classId") Long classId,
                                                     @Param("semesterId") Long semesterId,
                                                     @Param("limit") Integer limit);
}
