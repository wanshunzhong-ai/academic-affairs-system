package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.dto.query.OfferingQuery;
import com.aas.entity.CourseOffering;
import com.aas.entity.CourseSchedule;
import com.aas.mapper.CourseOfferingMapper;
import com.aas.mapper.CourseScheduleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 开课与排课服务
 */
@Service
@RequiredArgsConstructor
public class CourseOfferingService {

    private final CourseOfferingMapper offeringMapper;
    private final CourseScheduleMapper scheduleMapper;

    private static final String[] WEEK_TEXT = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    // ==================== 开课管理 ====================

    public PageResult<CourseOffering> page(OfferingQuery query) {
        Page<CourseOffering> page = query.toPage();
        PageResult<CourseOffering> result = PageResult.of(offeringMapper.selectOfferingPage(page, query));
        fillSchedules(result.getRecords());
        return result;
    }

    public CourseOffering detail(Long id) {
        CourseOffering offering = offeringMapper.selectOfferingDetail(id);
        if (offering == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        fillSchedules(List.of(offering));
        return offering;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(CourseOffering offering, List<CourseSchedule> schedules) {
        validate(offering);
        String code = buildOfferingCode(offering);
        offering.setOfferingCode(code);
        Long dup = offeringMapper.selectCount(new LambdaQueryWrapper<CourseOffering>()
                .eq(CourseOffering::getOfferingCode, code));
        if (dup != null && dup > 0) {
            throw new BizException("该班级在当前学期已开设此课程，请勿重复添加");
        }
        if (offering.getSelectedCount() == null) {
            offering.setSelectedCount(0);
        }
        if (offering.getIsPublic() == null) {
            offering.setIsPublic(offering.getClassId() == null ? 1 : 0);
        }
        offeringMapper.insert(offering);
        saveSchedules(offering, schedules);
        return offering.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(CourseOffering offering, List<CourseSchedule> schedules) {
        CourseOffering exists = offeringMapper.selectById(offering.getId());
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        validate(offering);
        offering.setOfferingCode(exists.getOfferingCode());
        offering.setSelectedCount(exists.getSelectedCount());
        offeringMapper.updateById(offering);
        if (schedules != null) {
            scheduleMapper.deleteByOfferingId(offering.getId());
            CourseOffering merged = offeringMapper.selectById(offering.getId());
            saveSchedules(merged, schedules);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Integer selected = offeringMapper.countSelected(id);
        if (selected != null && selected > 0) {
            throw new BizException("该开课已有学生选课，无法删除；请先清理选课记录或改为结课");
        }
        scheduleMapper.deleteByOfferingId(id);
        offeringMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        ids.forEach(this::delete);
    }

    /** 发布 / 结课 */
    public void changeStatus(Long id, Integer status) {
        CourseOffering offering = offeringMapper.selectById(id);
        if (offering == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        offeringMapper.update(null, new LambdaUpdateWrapper<CourseOffering>()
                .eq(CourseOffering::getId, id)
                .set(CourseOffering::getStatus, status));
    }

    /**
     * 保存排课(含冲突校验)
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveSchedules(CourseOffering offering, List<CourseSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return;
        }
        for (CourseSchedule schedule : schedules) {
            normalized(schedule);
            checkConflict(offering, schedule);
            schedule.setId(null);
            schedule.setOfferingId(offering.getId());
            scheduleMapper.insert(schedule);
        }
    }

    /** 单独保存排课 */
    @Transactional(rollbackFor = Exception.class)
    public void saveSchedules(Long offeringId, List<CourseSchedule> schedules) {
        CourseOffering offering = offeringMapper.selectById(offeringId);
        if (offering == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        scheduleMapper.deleteByOfferingId(offeringId);
        saveSchedules(offering, schedules);
    }

    // ==================== 课表查询 ====================

    /** 学生课表 */
    public List<CourseOffering> studentTimetable(Long studentId, Long semesterId) {
        List<CourseOffering> list = offeringMapper.selectStudentTimetable(studentId, semesterId);
        fillSchedules(list);
        return list;
    }

    /** 教师课表 */
    public List<CourseOffering> teacherTimetable(Long teacherId, Long semesterId) {
        List<CourseOffering> list = offeringMapper.selectTeacherTimetable(teacherId, semesterId);
        fillSchedules(list);
        return list;
    }

    /** 班级课表 */
    public List<CourseOffering> classTimetable(Long classId, Long semesterId) {
        List<CourseOffering> list = offeringMapper.selectClassTimetable(classId, semesterId);
        fillSchedules(list);
        return list;
    }

    /** 学生可选课程 */
    public PageResult<CourseOffering> selectableOfferings(Long studentId, Long classId, Long semesterId,
                                                          String keyword, com.aas.dto.PageQuery pageQuery) {
        Page<CourseOffering> page = pageQuery.toPage();
        PageResult<CourseOffering> result = PageResult.of(
                offeringMapper.selectSelectableOfferings(page, studentId, classId, semesterId, keyword));
        fillSchedules(result.getRecords());
        return result;
    }

    // ==================== 内部方法 ====================

    /** 批量填充排课信息与冲突文本 */
    public void fillSchedules(List<CourseOffering> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> ids = list.stream().map(CourseOffering::getId).collect(Collectors.toList());
        List<CourseSchedule> all = scheduleMapper.selectByOfferingIds(ids);
        Map<Long, List<CourseSchedule>> grouped = all.stream()
                .collect(Collectors.groupingBy(CourseSchedule::getOfferingId, LinkedHashMap::new, Collectors.toList()));
        for (CourseOffering offering : list) {
            List<CourseSchedule> schedules = grouped.getOrDefault(offering.getId(), new ArrayList<>());
            schedules.forEach(s -> s.setWeekDayText(weekText(s.getWeekDay())));
            offering.setSchedules(schedules);
            offering.setScheduleText(buildScheduleText(schedules));
        }
    }

    /** 拼装排课文本: 周一 第1-2节 第一教学楼-A01 */
    public static String buildScheduleText(List<CourseSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return "暂未排课";
        }
        List<String> parts = new ArrayList<>();
        for (CourseSchedule s : schedules) {
            StringBuilder sb = new StringBuilder();
            sb.append(weekText(s.getWeekDay()));
            sb.append(" 第").append(s.getStartSection());
            if (s.getEndSection() != null && !s.getEndSection().equals(s.getStartSection())) {
                sb.append("-").append(s.getEndSection());
            }
            sb.append("节");
            if (s.getClassroomName() != null) {
                sb.append(" ").append(s.getClassroomName());
            }
            parts.add(sb.toString());
        }
        return String.join("；", parts);
    }

    public static String weekText(Integer weekDay) {
        if (weekDay == null || weekDay < 1 || weekDay > 7) {
            return "";
        }
        return WEEK_TEXT[weekDay];
    }

    private void normalized(CourseSchedule schedule) {
        if (schedule.getWeekDay() == null || schedule.getWeekDay() < 1 || schedule.getWeekDay() > 7) {
            throw new BizException("请选择正确的上课星期");
        }
        if (schedule.getStartSection() == null || schedule.getEndSection() == null
                || schedule.getStartSection() > schedule.getEndSection()) {
            throw new BizException("节次区间不正确");
        }
        if (schedule.getStartWeek() == null) {
            schedule.setStartWeek(1);
        }
        if (schedule.getEndWeek() == null) {
            schedule.setEndWeek(16);
        }
    }

    /**
     * 排课冲突校验：教师 / 班级 / 教室
     */
    private void checkConflict(CourseOffering offering, CourseSchedule schedule) {
        List<Map<String, Object>> conflicts = scheduleMapper.selectConflicts(
                offering.getId(), offering.getSemesterId(),
                schedule.getWeekDay(), schedule.getStartSection(), schedule.getEndSection(),
                schedule.getStartWeek(), schedule.getEndWeek(),
                offering.getTeacherId(), offering.getClassId(), schedule.getClassroomId());
        if (conflicts == null || conflicts.isEmpty()) {
            return;
        }
        Map<String, Object> first = conflicts.get(0);
        String time = weekText(schedule.getWeekDay()) + " 第" + schedule.getStartSection()
                + "-" + schedule.getEndSection() + "节";
        String courseName = String.valueOf(first.get("courseName"));
        if (offering.getTeacherId() != null
                && offering.getTeacherId().equals(asLong(first.get("teacherId")))) {
            throw new BizException("排课冲突：" + time + " 授课教师已被《" + courseName + "》占用");
        }
        if (offering.getClassId() != null
                && offering.getClassId().equals(asLong(first.get("classId")))) {
            throw new BizException("排课冲突：" + time + " 该班级已有《" + courseName + "》");
        }
        if (schedule.getClassroomId() != null
                && schedule.getClassroomId().equals(asLong(first.get("classroomId")))) {
            throw new BizException("排课冲突：" + time + " 教室已被《" + courseName + "》占用");
        }
        throw new BizException("排课冲突：" + time + " 与《" + courseName + "》时间重叠");
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String buildOfferingCode(CourseOffering offering) {
        long classPart = offering.getClassId() == null ? 0L : offering.getClassId();
        return String.format("OF%04d%04d%04d", offering.getSemesterId(), offering.getCourseId(), classPart);
    }

    private void validate(CourseOffering offering) {
        if (offering.getCourseId() == null) {
            throw new BizException("请选择课程");
        }
        if (offering.getSemesterId() == null) {
            throw new BizException("请选择学期");
        }
        if (offering.getCapacity() == null || offering.getCapacity() <= 0) {
            offering.setCapacity(60);
        }
        if (offering.getStatus() == null) {
            offering.setStatus(1);
        }
        if (offering.getIsPublic() == null) {
            offering.setIsPublic(0);
        }
    }

    /** 统计某学期的开课数 */
    public long countBySemester(Long semesterId) {
        return offeringMapper.selectCount(new LambdaQueryWrapper<CourseOffering>()
                .eq(semesterId != null, CourseOffering::getSemesterId, semesterId));
    }

    /** 公共选修课列表 */
    public List<CourseOffering> listPublicOfferings(Long semesterId) {
        List<CourseOffering> list = offeringMapper.selectList(new LambdaQueryWrapper<CourseOffering>()
                .eq(semesterId != null, CourseOffering::getSemesterId, semesterId)
                .eq(CourseOffering::getIsPublic, 1)
                .eq(CourseOffering::getStatus, 1));
        fillSchedules(list);
        return list;
    }

    public List<CourseSchedule> schedules(Long offeringId) {
        return scheduleMapper.selectByOfferingId(offeringId);
    }

    public List<CourseSchedule> emptySchedules() {
        return Collections.emptyList();
    }
}
