package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.dto.query.CommonQuery;
import com.aas.entity.BaseDept;
import com.aas.entity.CourseCourse;
import com.aas.mapper.BaseDeptMapper;
import com.aas.mapper.CourseCourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程服务
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseCourseMapper courseMapper;
    private final BaseDeptMapper deptMapper;

    public PageResult<CourseCourse> page(CommonQuery query) {
        LambdaQueryWrapper<CourseCourse> wrapper = new LambdaQueryWrapper<CourseCourse>()
                .and(query.getKeyword() != null && !query.getKeyword().isBlank(), w -> w
                        .like(CourseCourse::getCourseName, query.getKeyword())
                        .or().like(CourseCourse::getCourseCode, query.getKeyword()))
                .eq(query.getDeptId() != null, CourseCourse::getDeptId, query.getDeptId())
                .eq(query.getCourseType() != null && !query.getCourseType().isBlank(),
                        CourseCourse::getCourseType, query.getCourseType())
                .eq(query.getStatus() != null, CourseCourse::getStatus, query.getStatus())
                .orderByAsc(CourseCourse::getCourseCode);
        Page<CourseCourse> page = query.toPage();
        PageResult<CourseCourse> result = PageResult.of(courseMapper.selectPage(page, wrapper));
        fillDeptName(result.getRecords());
        return result;
    }

    public List<CourseCourse> listAll() {
        List<CourseCourse> list = courseMapper.selectList(new LambdaQueryWrapper<CourseCourse>()
                .eq(CourseCourse::getStatus, 1)
                .orderByAsc(CourseCourse::getCourseCode));
        fillDeptName(list);
        return list;
    }

    public CourseCourse detail(Long id) {
        CourseCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        fillDeptName(List.of(course));
        return course;
    }

    public Long create(CourseCourse course) {
        checkCodeUnique(course.getCourseCode(), null);
        validate(course);
        courseMapper.insert(course);
        return course.getId();
    }

    public void update(CourseCourse course) {
        if (courseMapper.selectById(course.getId()) == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        checkCodeUnique(course.getCourseCode(), course.getId());
        validate(course);
        courseMapper.updateById(course);
    }

    public void delete(Long id) {
        courseMapper.deleteById(id);
    }

    public void changeStatus(Long id, Integer status) {
        CourseCourse course = new CourseCourse();
        course.setId(id);
        course.setStatus(status);
        courseMapper.updateById(course);
    }

    private void validate(CourseCourse course) {
        if (course.getCourseName() == null || course.getCourseName().isBlank()) {
            throw new BizException("课程名称不能为空");
        }
        if (course.getCredit() == null || course.getCredit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("学分必须大于0");
        }
        if (course.getStatus() == null) {
            course.setStatus(1);
        }
        if (course.getHours() == null) {
            course.setHours(course.getCredit().multiply(new BigDecimal("16")).intValue());
        }
    }

    private void fillDeptName(List<CourseCourse> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, String> map = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(BaseDept::getId, BaseDept::getDeptName, (a, b) -> a));
        list.forEach(c -> c.setDeptName(map.get(c.getDeptId())));
    }

    private void checkCodeUnique(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            throw new BizException("课程编号不能为空");
        }
        Long count = courseMapper.selectCount(new LambdaQueryWrapper<CourseCourse>()
                .eq(CourseCourse::getCourseCode, code)
                .ne(excludeId != null, CourseCourse::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("课程编号【" + code + "】已存在");
        }
    }
}
