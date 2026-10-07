package com.aas.service;

import com.aas.dto.DataScope;
import com.aas.entity.*;
import com.aas.mapper.*;
import com.aas.security.LoginUser;
import com.aas.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台 / 数据统计服务
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final StuStudentMapper studentMapper;
    private final TeaTeacherMapper teacherMapper;
    private final BaseClassMapper classMapper;
    private final BaseDeptMapper deptMapper;
    private final BaseMajorMapper majorMapper;
    private final CourseCourseMapper courseMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseSelectionMapper selectionMapper;
    private final ScoreRecordMapper scoreMapper;
    private final AttAttendanceMapper attendanceMapper;
    private final AttLeaveMapper leaveMapper;
    private final SysNoticeMapper noticeMapper;
    private final SysNoticeService noticeService;
    private final BaseSemesterService semesterService;
    private final CourseOfferingService offeringService;

    /**
     * 工作台首页数据（按角色返回不同内容）
     */
    public Map<String, Object> overview() {
        LoginUser user = SecurityUtils.getLoginUser();
        Map<String, Object> result = new HashMap<>();
        result.put("roleCode", user.getRoleCode());
        result.put("roleName", user.getRoleName());
        result.put("realName", user.getRealName());

        BaseSemester current = semesterService.current();
        result.put("currentSemester", current == null ? null : current.getSemesterName());
        result.put("currentSemesterId", current == null ? null : current.getId());
        result.put("unreadNotice", noticeService.unreadCount(user));

        if (user.isAdmin()) {
            result.putAll(adminOverview());
        } else if (user.isAcademic()) {
            result.putAll(academicOverview(current));
        } else if (user.isHeadTeacher()) {
            result.putAll(headTeacherOverview(user, current));
        } else {
            result.putAll(studentOverview(user, current));
        }
        return result;
    }

    /** 管理员视角：系统规模统计 */
    private Map<String, Object> adminOverview() {
        Map<String, Object> map = new HashMap<>();
        map.put("userCount", userMapper.selectCount(null));
        map.put("studentCount", studentMapper.selectCount(null));
        map.put("teacherCount", teacherMapper.selectCount(null));
        map.put("classCount", classMapper.selectCount(null));
        map.put("deptCount", deptMapper.selectCount(null));
        map.put("majorCount", majorMapper.selectCount(null));
        map.put("courseCount", courseMapper.selectCount(null));
        map.put("offeringCount", offeringMapper.selectCount(null));
        map.put("selectionCount", selectionMapper.selectCount(new LambdaQueryWrapper<CourseSelection>()
                .eq(CourseSelection::getStatus, 1)));
        map.put("noticeCount", noticeMapper.selectCount(null));
        map.put("roleCount", roleMapper.selectCount(null));
        return map;
    }

    /** 教务处视角：教务运行统计 */
    private Map<String, Object> academicOverview(BaseSemester current) {
        Map<String, Object> map = new HashMap<>();
        Long semesterId = current == null ? null : current.getId();
        map.put("studentCount", studentMapper.selectCount(null));
        map.put("teacherCount", teacherMapper.selectCount(null));
        map.put("classCount", classMapper.selectCount(null));
        map.put("courseCount", courseMapper.selectCount(null));
        map.put("offeringCount", offeringMapper.selectCount(new LambdaQueryWrapper<CourseOffering>()
                .eq(semesterId != null, CourseOffering::getSemesterId, semesterId)));
        map.put("selectionCount", selectionMapper.selectCount(new LambdaQueryWrapper<CourseSelection>()
                .eq(CourseSelection::getStatus, 1)));
        map.put("pendingLeave", leaveMapper.selectCount(new LambdaQueryWrapper<AttLeave>()
                .eq(AttLeave::getStatus, 0)));
        map.put("unpublishedScore", scoreMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .ne(ScoreRecord::getStatus, 2).isNotNull(ScoreRecord::getTotalScore)));
        map.put("abnormalAttendance", attendanceMapper.selectCount(new LambdaQueryWrapper<AttAttendance>()
                .ne(AttAttendance::getAttendType, 1)));
        map.put("deptDistribution", deptStudentDistribution());
        map.put("scoreDistribution", scoreMapper.selectSelectDistributionGlobal());
        map.put("classScoreRank", scoreMapper.selectCourseAvgStats(null, semesterId));
        return map;
    }

    /** 班主任视角 */
    private Map<String, Object> headTeacherOverview(LoginUser user, BaseSemester current) {
        Map<String, Object> map = new HashMap<>();
        List<Long> classIds = user.getManageClassIds() == null ? new ArrayList<>() : user.getManageClassIds();
        map.put("manageClassCount", classIds.size());
        map.put("manageClassNames", teacherMapper.selectManageClassNames(user.getTeacherId()));
        if (classIds.isEmpty()) {
            map.put("studentCount", 0);
            map.put("pendingLeave", 0);
            map.put("todayAttendance", 0);
            map.put("studentList", new ArrayList<>());
            map.put("avgScore", 0);
            return map;
        }
        Long studentCount = studentMapper.selectCount(new LambdaQueryWrapper<StuStudent>()
                .in(StuStudent::getClassId, classIds));
        map.put("studentCount", studentCount);

        DataScope scope = DataScope.of(user);
        map.put("pendingLeave", leaveMapper.countPending(scope));
        map.put("todayAttendance", attendanceMapper.selectCount(new LambdaQueryWrapper<AttAttendance>()
                .in(AttAttendance::getStudentId,
                        studentMapper.selectList(new LambdaQueryWrapper<StuStudent>()
                                        .in(StuStudent::getClassId, classIds))
                                .stream().map(StuStudent::getId).toList())
                .eq(AttAttendance::getAttendDate, LocalDate.now())));

        // 本班课程平均分
        Long semesterId = current == null ? null : current.getId();
        List<Map<String, Object>> stats = scoreMapper.selectCourseAvgStats(classIds.get(0), semesterId);
        map.put("classScoreRank", stats);
        double avg = stats.stream()
                .mapToDouble(s -> s.get("avgScore") == null ? 0 : Double.parseDouble(String.valueOf(s.get("avgScore"))))
                .average().orElse(0);
        map.put("avgScore", Math.round(avg * 100) / 100.0);

        // 本班学生名单(前10)
        List<StuStudent> students = studentMapper.selectList(new LambdaQueryWrapper<StuStudent>()
                .in(StuStudent::getClassId, classIds)
                .orderByAsc(StuStudent::getStudentNo)
                .last("LIMIT 10"));
        map.put("studentList", students);
        return map;
    }

    /** 学生视角 */
    private Map<String, Object> studentOverview(LoginUser user, BaseSemester current) {
        Map<String, Object> map = new HashMap<>();
        Long studentId = user.getStudentId();
        if (studentId == null) {
            return map;
        }
        Long semesterId = current == null ? null : current.getId();
        StuStudent student = studentMapper.selectStudentDetail(studentId);
        map.put("student", student);
        map.put("className", student == null ? null : student.getClassName());
        map.put("majorName", student == null ? null : student.getMajorName());
        map.put("deptName", student == null ? null : student.getDeptName());

        List<CourseOffering> timetable = offeringService.studentTimetable(studentId, semesterId);
        map.put("courseCount", timetable.size());
        map.put("timetable", timetable);

        BigDecimal credit = selectionMapper.sumSelectedCredit(studentId, semesterId);
        map.put("currentCredit", credit == null ? BigDecimal.ZERO : credit);

        Map<String, Object> summary = scoreMapper.selectStudentSummary(studentId);
        map.put("scoreSummary", summary == null ? new HashMap<>() : summary);

        map.put("pendingLeave", leaveMapper.selectCount(new LambdaQueryWrapper<AttLeave>()
                .eq(AttLeave::getStudentId, studentId).eq(AttLeave::getStatus, 0)));
        map.put("myLeaveCount", leaveMapper.selectCount(new LambdaQueryWrapper<AttLeave>()
                .eq(AttLeave::getStudentId, studentId)));

        Map<String, Object> attendance = new HashMap<>();
        List<Map<String, Object>> attStats = attendanceMapper.selectStudentStats(studentId);
        attendance.put("details", attStats);
        long abnormal = attStats.stream()
                .filter(r -> r.get("name") != null && !"出勤".equals(String.valueOf(r.get("name"))))
                .mapToLong(r -> Long.parseLong(String.valueOf(r.get("value"))))
                .sum();
        attendance.put("abnormal", abnormal);
        map.put("attendance", attendance);

        map.put("scoreDistribution", scoreMapper.selectStudentScoreDistribution(studentId));
        return map;
    }

    /** 各院系学生人数分布 */
    public List<Map<String, Object>> deptStudentDistribution() {
        List<BaseDept> depts = deptMapper.selectList(new LambdaQueryWrapper<BaseDept>()
                .orderByAsc(BaseDept::getSort));
        List<Map<String, Object>> result = new ArrayList<>();
        for (BaseDept dept : depts) {
            Long count = studentMapper.selectCount(new LambdaQueryWrapper<StuStudent>()
                    .eq(StuStudent::getDeptId, dept.getId()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", dept.getDeptName());
            row.put("value", count == null ? 0 : count);
            result.add(row);
        }
        return result;
    }

    /** 各专业学生人数分布 */
    public List<Map<String, Object>> majorStudentDistribution() {
        List<BaseMajor> majors = majorMapper.selectList(new LambdaQueryWrapper<BaseMajor>()
                .orderByAsc(BaseMajor::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (BaseMajor major : majors) {
            Long count = studentMapper.selectCount(new LambdaQueryWrapper<StuStudent>()
                    .eq(StuStudent::getMajorId, major.getId()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", major.getMajorName());
            row.put("value", count == null ? 0 : count);
            result.add(row);
        }
        return result;
    }

    /** 班级人数分布 */
    public List<Map<String, Object>> classDistribution() {
        List<BaseClass> classes = classMapper.selectList(new LambdaQueryWrapper<BaseClass>()
                .orderByAsc(BaseClass::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (BaseClass clazz : classes) {
            Long count = studentMapper.selectCount(new LambdaQueryWrapper<StuStudent>()
                    .eq(StuStudent::getClassId, clazz.getId()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", clazz.getClassName());
            row.put("value", count == null ? 0 : count);
            result.add(row);
        }
        return result;
    }
}
