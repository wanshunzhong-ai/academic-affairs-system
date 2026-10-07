package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.exception.BizException;
import com.aas.dto.DataScope;
import com.aas.dto.query.AttendanceQuery;
import com.aas.entity.AttAttendance;
import com.aas.entity.CourseSelection;
import com.aas.mapper.AttAttendanceMapper;
import com.aas.mapper.CourseSelectionMapper;
import com.aas.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤服务
 */
@Service
@RequiredArgsConstructor
public class AttAttendanceService {

    private final AttAttendanceMapper attendanceMapper;
    private final CourseSelectionMapper selectionMapper;

    public PageResult<AttAttendance> page(AttendanceQuery query, DataScope scope) {
        Page<AttAttendance> page = query.toPage();
        return PageResult.of(attendanceMapper.selectAttendancePage(page, query, scope));
    }

    /**
     * 新增考勤记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(AttAttendance record) {
        if (record.getStudentId() == null) {
            throw new BizException("请选择学生");
        }
        if (record.getAttendDate() == null) {
            record.setAttendDate(LocalDate.now());
        }
        if (record.getAttendType() == null) {
            record.setAttendType(1);
        }
        record.setRecorderId(SecurityUtils.getUserIdOrNull());
        attendanceMapper.insert(record);
    }

    /**
     * 批量记录考勤（对某开课的一次点名）
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchRecord(Long offeringId, LocalDate date, List<AttAttendance> records) {
        if (offeringId == null) {
            throw new BizException("请选择课程");
        }
        if (date == null) {
            date = LocalDate.now();
        }
        if (records == null || records.isEmpty()) {
            throw new BizException("没有需要保存的考勤数据");
        }
        Long recorder = SecurityUtils.getUserIdOrNull();
        int count = 0;
        for (AttAttendance record : records) {
            record.setId(null);
            record.setOfferingId(offeringId);
            record.setAttendDate(date);
            record.setRecorderId(recorder);
            if (record.getAttendType() == null) {
                record.setAttendType(1);
            }
            attendanceMapper.insert(record);
            count++;
        }
        return count;
    }

    /**
     * 一键为某开课的选课学生生成考勤草稿
     */
    public List<Map<String, Object>> draftForOffering(Long offeringId) {
        List<CourseSelection> students = selectionMapper.selectByOffering(offeringId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CourseSelection selection : students) {
            Map<String, Object> row = new HashMap<>();
            row.put("studentId", selection.getStudentId());
            row.put("studentNo", selection.getStudentNo());
            row.put("studentName", selection.getStudentName());
            row.put("attendType", 1);
            result.add(row);
        }
        return result;
    }

    public void update(AttAttendance record) {
        if (attendanceMapper.selectById(record.getId()) == null) {
            throw new BizException("考勤记录不存在");
        }
        attendanceMapper.updateById(record);
    }

    public void delete(Long id) {
        attendanceMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        attendanceMapper.deleteByIds(ids);
    }

    /** 学生考勤统计 */
    public List<Map<String, Object>> studentStats(Long studentId) {
        return attendanceMapper.selectStudentStats(studentId);
    }

    /** 班级考勤统计 */
    public List<Map<String, Object>> classStats(Long classId) {
        return attendanceMapper.selectClassStats(classId);
    }

    /** 学生考勤汇总(用于学生工作台) */
    public Map<String, Object> mySummary(Long studentId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> stats = attendanceMapper.selectStudentStats(studentId);
        long total = 0;
        long abnormal = 0;
        for (Map<String, Object> row : stats) {
            long value = row.get("value") == null ? 0 : Long.parseLong(String.valueOf(row.get("value")));
            total += value;
            Object name = row.get("name");
            if (name != null && !"出勤".equals(String.valueOf(name))) {
                abnormal += value;
            }
        }
        result.put("total", total);
        result.put("abnormal", abnormal);
        result.put("details", stats);
        return result;
    }

    /** 统计某日期区间的考勤异常数 */
    public long countAbnormal(LocalDate start, LocalDate end) {
        return attendanceMapper.selectCount(new LambdaQueryWrapper<AttAttendance>()
                .ne(AttAttendance::getAttendType, 1)
                .ge(start != null, AttAttendance::getAttendDate, start)
                .le(end != null, AttAttendance::getAttendDate, end));
    }
}
