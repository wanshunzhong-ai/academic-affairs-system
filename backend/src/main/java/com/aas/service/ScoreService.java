package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.exception.BizException;
import com.aas.dto.DataScope;
import com.aas.dto.query.ScoreQuery;
import com.aas.entity.CourseOffering;
import com.aas.entity.CourseSelection;
import com.aas.entity.ScoreRecord;
import com.aas.mapper.CourseOfferingMapper;
import com.aas.mapper.CourseSelectionMapper;
import com.aas.mapper.ScoreRecordMapper;
import com.aas.security.LoginUser;
import com.aas.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成绩服务
 */
@Service
@RequiredArgsConstructor
public class ScoreService {

    /** 平时成绩占比 */
    private static final BigDecimal USUAL_RATIO = new BigDecimal("0.3");
    /** 期末成绩占比 */
    private static final BigDecimal EXAM_RATIO = new BigDecimal("0.7");

    private final ScoreRecordMapper scoreMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseSelectionMapper selectionMapper;

    // ==================== 查询 ====================

    public PageResult<ScoreRecord> page(ScoreQuery query, DataScope scope) {
        Page<ScoreRecord> page = query.toPage();
        PageResult<ScoreRecord> result = PageResult.of(scoreMapper.selectScorePage(page, query, scope));
        result.getRecords().forEach(this::fillLevel);
        return result;
    }

    /** 学生成绩单 */
    public List<ScoreRecord> studentScores(Long studentId, Long semesterId, boolean onlyPublished) {
        List<ScoreRecord> list = scoreMapper.selectByStudent(studentId, semesterId);
        if (onlyPublished) {
            list = list.stream().filter(s -> s.getStatus() != null && s.getStatus() == 2).toList();
        }
        list.forEach(this::fillLevel);
        return list;
    }

    /** 某开课的成绩单 */
    public List<ScoreRecord> offeringScores(Long offeringId) {
        List<ScoreRecord> list = scoreMapper.selectByOffering(offeringId);
        list.forEach(this::fillLevel);
        return list;
    }

    /** 课程成绩分布 */
    public List<Map<String, Object>> distribution(Long offeringId) {
        return scoreMapper.selectScoreDistribution(offeringId);
    }

    /** 课程平均分统计 */
    public List<Map<String, Object>> courseStats(Long classId, Long semesterId) {
        return scoreMapper.selectCourseAvgStats(classId, semesterId);
    }

    /** 开课成绩概览 */
    public Map<String, Object> offeringSummary(Long offeringId) {
        Map<String, Object> map = scoreMapper.selectOfferingSummary(offeringId);
        return map == null ? new HashMap<>() : map;
    }

    // ==================== 录入 ====================

    /**
     * 为某开课初始化成绩记录(按已选学生生成空记录)
     */
    @Transactional(rollbackFor = Exception.class)
    public int initForOffering(Long offeringId) {
        CourseOffering offering = offeringMapper.selectOfferingDetail(offeringId);
        if (offering == null) {
            throw new BizException("开课信息不存在");
        }
        List<CourseSelection> students = selectionMapper.selectByOffering(offeringId);
        if (students.isEmpty()) {
            throw new BizException("该课程暂无学生选课，无法生成成绩单");
        }
        int created = 0;
        for (CourseSelection selection : students) {
            Long exists = scoreMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                    .eq(ScoreRecord::getStudentId, selection.getStudentId())
                    .eq(ScoreRecord::getOfferingId, offeringId));
            if (exists != null && exists > 0) {
                continue;
            }
            ScoreRecord record = new ScoreRecord();
            record.setStudentId(selection.getStudentId());
            record.setOfferingId(offeringId);
            record.setStatus(0);
            record.setIsRetake(0);
            scoreMapper.insert(record);
            created++;
        }
        return created;
    }

    /**
     * 批量保存成绩
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(Long offeringId, List<ScoreRecord> records) {
        CourseOffering offering = offeringMapper.selectOfferingDetail(offeringId);
        if (offering == null) {
            throw new BizException("开课信息不存在");
        }
        checkInputPermission(offering);
        if (records == null || records.isEmpty()) {
            throw new BizException("没有需要保存的成绩数据");
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        for (ScoreRecord record : records) {
            computeTotal(record);
            record.setOfferingId(offeringId);
            record.setInputBy(loginUser.getUserId());
            record.setInputTime(LocalDateTime.now());
            if (record.getStatus() == null || record.getStatus() == 2) {
                record.setStatus(1);
            }
            if (record.getId() != null) {
                ScoreRecord exists = scoreMapper.selectById(record.getId());
                if (exists != null && exists.getStatus() != null && exists.getStatus() == 2) {
                    throw new BizException("已发布的成绩不可修改，请先撤回发布");
                }
                scoreMapper.updateById(record);
            } else {
                ScoreRecord exists = scoreMapper.selectOne(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getStudentId, record.getStudentId())
                        .eq(ScoreRecord::getOfferingId, offeringId)
                        .last("LIMIT 1"));
                if (exists != null) {
                    record.setId(exists.getId());
                    scoreMapper.updateById(record);
                } else {
                    scoreMapper.insert(record);
                }
            }
        }
    }

    /**
     * 单条成绩录入/修改
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOne(ScoreRecord record) {
        if (record.getOfferingId() == null) {
            throw new BizException("缺少开课信息");
        }
        CourseOffering offering = offeringMapper.selectOfferingDetail(record.getOfferingId());
        if (offering == null) {
            throw new BizException("开课信息不存在");
        }
        checkInputPermission(offering);
        computeTotal(record);
        record.setInputBy(SecurityUtils.getUserId());
        record.setInputTime(LocalDateTime.now());
        if (record.getId() == null) {
            ScoreRecord exists = scoreMapper.selectOne(new LambdaQueryWrapper<ScoreRecord>()
                    .eq(ScoreRecord::getStudentId, record.getStudentId())
                    .eq(ScoreRecord::getOfferingId, record.getOfferingId())
                    .last("LIMIT 1"));
            if (exists != null) {
                record.setId(exists.getId());
            }
        }
        if (record.getStatus() == null) {
            record.setStatus(1);
        }
        if (record.getId() != null) {
            scoreMapper.updateById(record);
        } else {
            scoreMapper.insert(record);
        }
    }

    /**
     * 发布成绩
     */
    @Transactional(rollbackFor = Exception.class)
    public int publish(Long offeringId, List<Long> ids) {
        LambdaUpdateWrapper<ScoreRecord> wrapper = new LambdaUpdateWrapper<ScoreRecord>()
                .set(ScoreRecord::getStatus, 2)
                .set(ScoreRecord::getPublishTime, LocalDateTime.now());
        if (offeringId != null) {
            CourseOffering offering = offeringMapper.selectOfferingDetail(offeringId);
            if (offering == null) {
                throw new BizException("开课信息不存在");
            }
            checkInputPermission(offering);
            wrapper.eq(ScoreRecord::getOfferingId, offeringId);
        } else if (ids != null && !ids.isEmpty()) {
            wrapper.in(ScoreRecord::getId, ids);
        } else {
            throw new BizException("请选择要发布的成绩");
        }
        wrapper.isNotNull(ScoreRecord::getTotalScore);
        return scoreMapper.update(null, wrapper);
    }

    /**
     * 撤回发布
     */
    @Transactional(rollbackFor = Exception.class)
    public int revoke(Long offeringId, List<Long> ids) {
        LambdaUpdateWrapper<ScoreRecord> wrapper = new LambdaUpdateWrapper<ScoreRecord>()
                .set(ScoreRecord::getStatus, 1)
                .set(ScoreRecord::getPublishTime, null);
        if (offeringId != null) {
            wrapper.eq(ScoreRecord::getOfferingId, offeringId);
        } else if (ids != null && !ids.isEmpty()) {
            wrapper.in(ScoreRecord::getId, ids);
        } else {
            throw new BizException("请选择要撤回的成绩");
        }
        return scoreMapper.update(null, wrapper);
    }

    /**
     * 删除成绩记录
     */
    public void delete(Long id) {
        ScoreRecord record = scoreMapper.selectById(id);
        if (record == null) {
            throw new BizException("成绩记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 2) {
            throw new BizException("已发布的成绩不可删除");
        }
        scoreMapper.deleteById(id);
    }

    // ==================== 统计 ====================

    /** 成绩等级分布(全局) */
    public List<Map<String, Object>> levelDistribution() {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] levels = {"优秀(90-100)", "良好(80-89)", "中等(70-79)", "及格(60-69)", "不及格(<60)"};
        for (String level : levels) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", level);
            row.put("value", countByLevel(level));
            result.add(row);
        }
        return result;
    }

    /** 某学生成绩等级分布 */
    public List<Map<String, Object>> distributionByStudent(Long studentId) {
        if (studentId == null) {
            return new ArrayList<>();
        }
        return scoreMapper.selectStudentScoreDistribution(studentId);
    }

    /**
     * 学生成绩趋势（按学期平均分 / 平均绩点）
     */
    public List<Map<String, Object>> studentTrend(Long studentId) {
        if (studentId == null) {
            return new ArrayList<>();
        }
        List<ScoreRecord> records = scoreMapper.selectByStudent(studentId, null);
        Map<String, List<ScoreRecord>> grouped = new java.util.LinkedHashMap<>();
        for (ScoreRecord record : records) {
            if (record.getTotalScore() == null || record.getSemesterName() == null) {
                continue;
            }
            grouped.computeIfAbsent(record.getSemesterName(), k -> new ArrayList<>()).add(record);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((semester, list) -> {
            double avg = list.stream().mapToDouble(r -> r.getTotalScore().doubleValue()).average().orElse(0);
            double gp = list.stream()
                    .filter(r -> r.getGradePoint() != null)
                    .mapToDouble(r -> r.getGradePoint().doubleValue()).average().orElse(0);
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("semester", semester);
            row.put("avgScore", Math.round(avg * 100) / 100.0);
            row.put("avgPoint", Math.round(gp * 100) / 100.0);
            row.put("courseCount", list.size());
            result.add(row);
        });
        return result;
    }

    /** 班级学生成绩排名 */
    public List<Map<String, Object>> classRank(Long classId, Long semesterId, Integer limit) {
        if (classId == null) {
            return new ArrayList<>();
        }
        return scoreMapper.selectClassStudentRank(classId, semesterId, limit == null ? 20 : limit);
    }

    private long countByLevel(String level) {
        LambdaQueryWrapper<ScoreRecord> wrapper = new LambdaQueryWrapper<ScoreRecord>()
                .isNotNull(ScoreRecord::getTotalScore);
        switch (level) {
            case "优秀(90-100)" -> wrapper.ge(ScoreRecord::getTotalScore, 90);
            case "良好(80-89)" -> wrapper.ge(ScoreRecord::getTotalScore, 80).lt(ScoreRecord::getTotalScore, 90);
            case "中等(70-79)" -> wrapper.ge(ScoreRecord::getTotalScore, 70).lt(ScoreRecord::getTotalScore, 80);
            case "及格(60-69)" -> wrapper.ge(ScoreRecord::getTotalScore, 60).lt(ScoreRecord::getTotalScore, 70);
            default -> wrapper.lt(ScoreRecord::getTotalScore, 60);
        }
        return scoreMapper.selectCount(wrapper);
    }

    // ==================== 内部方法 ====================

    /** 计算总评与绩点 */
    private void computeTotal(ScoreRecord record) {
        BigDecimal usual = record.getUsualScore();
        BigDecimal exam = record.getExamScore();
        if (usual != null || exam != null) {
            BigDecimal u = usual == null ? BigDecimal.ZERO : usual;
            BigDecimal e = exam == null ? BigDecimal.ZERO : exam;
            BigDecimal total;
            if (usual != null && exam != null) {
                total = u.multiply(USUAL_RATIO).add(e.multiply(EXAM_RATIO));
            } else if (exam != null) {
                total = e;
            } else {
                total = u;
            }
            total = total.setScale(1, RoundingMode.HALF_UP);
            record.setTotalScore(total);
            record.setGradePoint(calcGradePoint(total));
        }
        if (record.getTotalScore() != null
                && (record.getTotalScore().compareTo(BigDecimal.ZERO) < 0
                || record.getTotalScore().compareTo(new BigDecimal("100")) > 0)) {
            throw new BizException("成绩必须在 0-100 之间");
        }
    }

    /** 绩点换算 */
    public static BigDecimal calcGradePoint(BigDecimal total) {
        if (total == null) {
            return null;
        }
        double t = total.doubleValue();
        double gp;
        if (t >= 90) {
            gp = 4.0;
        } else if (t >= 85) {
            gp = 3.7;
        } else if (t >= 82) {
            gp = 3.3;
        } else if (t >= 78) {
            gp = 3.0;
        } else if (t >= 75) {
            gp = 2.7;
        } else if (t >= 72) {
            gp = 2.3;
        } else if (t >= 68) {
            gp = 2.0;
        } else if (t >= 64) {
            gp = 1.5;
        } else if (t >= 60) {
            gp = 1.0;
        } else {
            gp = 0.0;
        }
        return BigDecimal.valueOf(gp).setScale(2, RoundingMode.HALF_UP);
    }

    private void fillLevel(ScoreRecord record) {
        if (record.getTotalScore() == null) {
            record.setLevel("未录入");
            return;
        }
        double t = record.getTotalScore().doubleValue();
        if (t >= 90) {
            record.setLevel("优秀");
        } else if (t >= 80) {
            record.setLevel("良好");
        } else if (t >= 70) {
            record.setLevel("中等");
        } else if (t >= 60) {
            record.setLevel("及格");
        } else {
            record.setLevel("不及格");
        }
    }

    /**
     * 成绩录入权限校验：
     * 管理员/教务处可录入全部；班主任仅可录入本班或本人授课的课程成绩
     */
    private void checkInputPermission(CourseOffering offering) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user.isAdmin() || user.isAcademic()) {
            return;
        }
        boolean isOwnCourse = offering.getTeacherId() != null
                && offering.getTeacherId().equals(user.getTeacherId());
        boolean isOwnClass = offering.getClassId() != null
                && user.getManageClassIds() != null
                && user.getManageClassIds().contains(offering.getClassId());
        if (!isOwnCourse && !isOwnClass) {
            throw new BizException("您只能录入本人任教或本班课程的成记录");
        }
    }
}
