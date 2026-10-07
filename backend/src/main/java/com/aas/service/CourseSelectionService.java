package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.exception.BizException;
import com.aas.dto.DataScope;
import com.aas.dto.query.SelectionQuery;
import com.aas.entity.CourseOffering;
import com.aas.entity.CourseSchedule;
import com.aas.entity.CourseSelection;
import com.aas.mapper.CourseOfferingMapper;
import com.aas.mapper.CourseSelectionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 选课服务
 */
@Service
@RequiredArgsConstructor
public class CourseSelectionService {

    /** 单学期选课学分上限 */
    private static final BigDecimal MAX_CREDIT = new BigDecimal("30");

    private final CourseSelectionMapper selectionMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseOfferingService offeringService;
    private final BaseSemesterService semesterService;

    public PageResult<CourseSelection> page(SelectionQuery query, DataScope scope) {
        Page<CourseSelection> page = query.toPage();
        return PageResult.of(selectionMapper.selectSelectionPage(page, query, scope));
    }

    /** 学生自己的选课记录 */
    public List<CourseSelection> mySelections(Long studentId, Long semesterId) {
        return selectionMapper.selectByStudent(studentId, semesterId);
    }

    /** 某开课的选课学生名单 */
    public List<CourseSelection> offeringStudents(Long offeringId) {
        return selectionMapper.selectByOffering(offeringId);
    }

    /**
     * 学生选课
     */
    @Transactional(rollbackFor = Exception.class)
    public void select(Long studentId, Long offeringId) {
        CourseOffering offering = offeringMapper.selectOfferingDetail(offeringId);
        if (offering == null) {
            throw new BizException("开课信息不存在");
        }
        if (offering.getStatus() == null || offering.getStatus() != 1) {
            throw new BizException("该课程当前未开放选课");
        }
        // 选课时间窗口
        if (!semesterService.inSelectWindow(offering.getSemesterId())) {
            throw new BizException("当前不在选课时间范围内");
        }
        // 容量
        Integer selected = offeringMapper.countSelected(offeringId);
        if (selected != null && offering.getCapacity() != null && selected >= offering.getCapacity()) {
            throw new BizException("该课程选课人数已满");
        }

        // 是否已选(含已退选记录)
        CourseSelection exists = selectionMapper.selectOne(new LambdaQueryWrapper<CourseSelection>()
                .eq(CourseSelection::getStudentId, studentId)
                .eq(CourseSelection::getOfferingId, offeringId)
                .last("LIMIT 1"));
        if (exists != null && exists.getStatus() != null && exists.getStatus() == 1) {
            throw new BizException("您已选修该课程，请勿重复选课");
        }

        // 时间冲突校验
        checkTimeConflict(studentId, offering);

        // 学分上限校验
        BigDecimal current = selectionMapper.sumSelectedCredit(studentId, offering.getSemesterId());
        BigDecimal after = (current == null ? BigDecimal.ZERO : current).add(
                offering.getCredit() == null ? BigDecimal.ZERO : offering.getCredit());
        if (after.compareTo(MAX_CREDIT) > 0) {
            throw new BizException("选课后本学期待修学分将达到 " + after + " 分，超出上限 " + MAX_CREDIT + " 分");
        }

        if (exists != null) {
            selectionMapper.update(null, new LambdaUpdateWrapper<CourseSelection>()
                    .eq(CourseSelection::getId, exists.getId())
                    .set(CourseSelection::getStatus, 1)
                    .set(CourseSelection::getSelectType, 1)
                    .set(CourseSelection::getSelectTime, LocalDateTime.now()));
        } else {
            CourseSelection selection = new CourseSelection();
            selection.setStudentId(studentId);
            selection.setOfferingId(offeringId);
            selection.setSelectType(1);
            selection.setSelectTime(LocalDateTime.now());
            selection.setStatus(1);
            selectionMapper.insert(selection);
        }
        refreshSelectedCount(offeringId);
    }

    /**
     * 批量选课
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchSelect(Long studentId, List<Long> offeringIds) {
        if (offeringIds == null || offeringIds.isEmpty()) {
            throw new BizException("请选择要选修的课程");
        }
        for (Long offeringId : offeringIds) {
            select(studentId, offeringId);
        }
    }

    /**
     * 退课
     */
    @Transactional(rollbackFor = Exception.class)
    public void drop(Long studentId, Long offeringId) {
        CourseSelection selection = selectionMapper.selectOne(new LambdaQueryWrapper<CourseSelection>()
                .eq(CourseSelection::getStudentId, studentId)
                .eq(CourseSelection::getOfferingId, offeringId)
                .eq(CourseSelection::getStatus, 1)
                .last("LIMIT 1"));
        if (selection == null) {
            throw new BizException("未找到该选课记录");
        }
        CourseOffering offering = offeringMapper.selectById(offeringId);
        if (offering != null && !semesterService.inSelectWindow(offering.getSemesterId())) {
            throw new BizException("当前不在选课时间范围内，无法退课");
        }
        // 必修课不允许退选
        CourseOffering detail = offeringMapper.selectOfferingDetail(offeringId);
        if (detail != null && "必修".equals(detail.getCourseType())
                && detail.getClassId() != null) {
            throw new BizException("必修课由系统统一安排，不可退选");
        }
        selectionMapper.update(null, new LambdaUpdateWrapper<CourseSelection>()
                .eq(CourseSelection::getId, selection.getId())
                .set(CourseSelection::getStatus, 0));
        refreshSelectedCount(offeringId);
    }

    /**
     * 教务代选/移除
     */
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long offeringId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            throw new BizException("请选择学生");
        }
        for (Long studentId : studentIds) {
            CourseSelection exists = selectionMapper.selectOne(new LambdaQueryWrapper<CourseSelection>()
                    .eq(CourseSelection::getStudentId, studentId)
                    .eq(CourseSelection::getOfferingId, offeringId)
                    .last("LIMIT 1"));
            if (exists != null) {
                selectionMapper.update(null, new LambdaUpdateWrapper<CourseSelection>()
                        .eq(CourseSelection::getId, exists.getId())
                        .set(CourseSelection::getStatus, 1));
            } else {
                CourseSelection selection = new CourseSelection();
                selection.setStudentId(studentId);
                selection.setOfferingId(offeringId);
                selection.setSelectType(2);
                selection.setSelectTime(LocalDateTime.now());
                selection.setStatus(1);
                selectionMapper.insert(selection);
            }
        }
        refreshSelectedCount(offeringId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long selectionId) {
        CourseSelection selection = selectionMapper.selectById(selectionId);
        if (selection == null) {
            throw new BizException("选课记录不存在");
        }
        selectionMapper.deleteById(selectionId);
        refreshSelectedCount(selection.getOfferingId());
        syncScoreOnRemove(selection);
    }

    /** 同步开课已选人数 */
    public void refreshSelectedCount(Long offeringId) {
        Integer count = offeringMapper.countSelected(offeringId);
        CourseOffering update = new CourseOffering();
        update.setId(offeringId);
        update.setSelectedCount(count == null ? 0 : count);
        offeringMapper.updateById(update);
    }

    /** 学生选课总学分 */
    public BigDecimal myCredit(Long studentId, Long semesterId) {
        BigDecimal credit = selectionMapper.sumSelectedCredit(studentId, semesterId);
        return credit == null ? BigDecimal.ZERO : credit;
    }

    // ==================== 私有方法 ====================

    /** 检查与已选课程的时间冲突 */
    private void checkTimeConflict(Long studentId, CourseOffering target) {
        List<CourseSchedule> targetSchedules = offeringService.schedules(target.getId());
        if (targetSchedules.isEmpty()) {
            return;
        }
        List<CourseOffering> current = offeringService.studentTimetable(studentId, target.getSemesterId());
        List<String> conflicts = new ArrayList<>();
        for (CourseOffering offering : current) {
            if (offering.getId().equals(target.getId()) || offering.getSchedules() == null) {
                continue;
            }
            for (CourseSchedule exist : offering.getSchedules()) {
                for (CourseSchedule now : targetSchedules) {
                    if (overlap(exist, now)) {
                        conflicts.add(offering.getCourseName());
                        break;
                    }
                }
            }
        }
        if (!conflicts.isEmpty()) {
            throw new BizException("上课时间冲突，与已选课程《" + String.join("、", conflicts) + "》时间重叠");
        }
    }

    private boolean overlap(CourseSchedule a, CourseSchedule b) {
        if (a.getWeekDay() == null || b.getWeekDay() == null
                || !a.getWeekDay().equals(b.getWeekDay())) {
            return false;
        }
        boolean sectionOverlap = a.getStartSection() <= b.getEndSection()
                && a.getEndSection() >= b.getStartSection();
        if (!sectionOverlap) {
            return false;
        }
        int aStart = a.getStartWeek() == null ? 1 : a.getStartWeek();
        int aEnd = a.getEndWeek() == null ? 16 : a.getEndWeek();
        int bStart = b.getStartWeek() == null ? 1 : b.getStartWeek();
        int bEnd = b.getEndWeek() == null ? 16 : b.getEndWeek();
        return aStart <= bEnd && aEnd >= bStart;
    }

    /** 退课时同步清理未发布的成绩 */
    private void syncScoreOnRemove(CourseSelection selection) {
        // 由 ScoreService 负责，此处仅保留扩展点
    }
}
