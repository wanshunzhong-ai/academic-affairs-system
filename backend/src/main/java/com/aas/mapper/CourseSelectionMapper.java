package com.aas.mapper;

import com.aas.dto.DataScope;
import com.aas.dto.query.SelectionQuery;
import com.aas.entity.CourseSelection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseSelectionMapper extends BaseMapper<CourseSelection> {

    /** 分页查询选课记录 */
    IPage<CourseSelection> selectSelectionPage(IPage<CourseSelection> page,
                                               @Param("q") SelectionQuery query,
                                               @Param("scope") DataScope scope);

    /** 查询学生选课列表 */
    List<CourseSelection> selectByStudent(@Param("studentId") Long studentId,
                                          @Param("semesterId") Long semesterId);

    /** 查询某开课的全部选课学生 */
    List<CourseSelection> selectByOffering(@Param("offeringId") Long offeringId);

    /** 学生某学期的已选学分 */
    java.math.BigDecimal sumSelectedCredit(@Param("studentId") Long studentId,
                                           @Param("semesterId") Long semesterId);
}
