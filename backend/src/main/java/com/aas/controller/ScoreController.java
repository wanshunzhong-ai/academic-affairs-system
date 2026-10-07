package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.DataScope;
import com.aas.dto.query.ScoreQuery;
import com.aas.entity.ScoreRecord;
import com.aas.security.SecurityUtils;
import com.aas.service.ScoreService;
import com.aas.util.ExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 成绩管理
 */
@Tag(name = "16-成绩管理")
@RestController
@RequestMapping("/api/score")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    @Operation(summary = "分页查询成绩(自动应用数据权限)")
    @PreAuthorize("@ss.hasAnyPerm('score:input','score:audit','score:my')")
    @GetMapping("/page")
    public Result<PageResult<ScoreRecord>> page(ScoreQuery query) {
        return Result.success(scoreService.page(query, DataScope.of(SecurityUtils.getLoginUser())));
    }

    @Operation(summary = "我的成绩单")
    @PreAuthorize("@ss.hasPerm('score:my')")
    @GetMapping("/my")
    public Result<List<ScoreRecord>> my(@RequestParam(required = false) Long semesterId,
                                        @RequestParam(defaultValue = "true") Boolean onlyPublished) {
        return Result.success(scoreService.studentScores(SecurityUtils.getStudentId(), semesterId, onlyPublished));
    }

    @Operation(summary = "我的成绩等级分布")
    @PreAuthorize("@ss.hasPerm('score:my')")
    @GetMapping("/my/distribution")
    public Result<List<Map<String, Object>>> myDistribution() {
        return Result.success(scoreService.distributionByStudent(SecurityUtils.getStudentId()));
    }

    @Operation(summary = "我的成绩趋势(按学期平均分)")
    @PreAuthorize("@ss.hasPerm('score:my')")
    @GetMapping("/my/trend")
    public Result<List<Map<String, Object>>> myTrend() {
        return Result.success(scoreService.studentTrend(SecurityUtils.getStudentId()));
    }

    @Operation(summary = "某开课成绩单")
    @PreAuthorize("@ss.hasAnyPerm('score:input','score:audit')")
    @GetMapping("/offering/{offeringId}")
    public Result<List<ScoreRecord>> offeringScores(@PathVariable Long offeringId) {
        return Result.success(scoreService.offeringScores(offeringId));
    }

    @Operation(summary = "为开课生成成绩单(按选课学生)")
    @OperLog(module = "成绩管理", operation = "生成成绩单")
    @PreAuthorize("@ss.hasPerm('score:input')")
    @PostMapping("/offering/{offeringId}/init")
    public Result<Integer> init(@PathVariable Long offeringId) {
        int count = scoreService.initForOffering(offeringId);
        return Result.success("已生成 " + count + " 条成绩记录", count);
    }

    @Operation(summary = "批量保存成绩")
    @OperLog(module = "成绩管理", operation = "录入成绩")
    @PreAuthorize("@ss.hasPerm('score:input')")
    @PostMapping("/offering/{offeringId}/save")
    public Result<Void> saveBatch(@PathVariable Long offeringId, @RequestBody List<ScoreRecord> records) {
        scoreService.saveBatch(offeringId, records);
        return Result.success("保存成功", null);
    }

    @Operation(summary = "单条成绩录入/修改")
    @OperLog(module = "成绩管理", operation = "录入成绩")
    @PreAuthorize("@ss.hasPerm('score:input')")
    @PostMapping("/save")
    public Result<Void> saveOne(@RequestBody ScoreRecord record) {
        scoreService.saveOne(record);
        return Result.success("保存成功", null);
    }

    @Operation(summary = "发布成绩")
    @OperLog(module = "成绩管理", operation = "发布成绩")
    @PreAuthorize("@ss.hasAnyPerm('score:audit','score:input')")
    @PutMapping("/publish")
    public Result<Integer> publish(@RequestParam(required = false) Long offeringId,
                                   @RequestBody(required = false) List<Long> ids) {
        return Result.success("发布成功", scoreService.publish(offeringId, ids));
    }

    @Operation(summary = "撤回已发布成绩")
    @OperLog(module = "成绩管理", operation = "撤回成绩")
    @PreAuthorize("@ss.hasAnyPerm('score:audit','score:input')")
    @PutMapping("/revoke")
    public Result<Integer> revoke(@RequestParam(required = false) Long offeringId,
                                  @RequestBody(required = false) List<Long> ids) {
        return Result.success("撤回成功", scoreService.revoke(offeringId, ids));
    }

    @Operation(summary = "删除成绩记录")
    @OperLog(module = "成绩管理", operation = "删除成绩")
    @PreAuthorize("@ss.hasPerm('score:input')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scoreService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "开课成绩概览")
    @PreAuthorize("@ss.hasAnyPerm('score:input','score:audit','score:stat')")
    @GetMapping("/offering/{offeringId}/summary")
    public Result<Map<String, Object>> offeringSummary(@PathVariable Long offeringId) {
        return Result.success(scoreService.offeringSummary(offeringId));
    }

    @Operation(summary = "开课成绩分布")
    @PreAuthorize("@ss.hasAnyPerm('score:input','score:audit','score:stat')")
    @GetMapping("/offering/{offeringId}/distribution")
    public Result<List<Map<String, Object>>> distribution(@PathVariable Long offeringId) {
        return Result.success(scoreService.distribution(offeringId));
    }

    // ==================== 统计 ====================

    @Operation(summary = "课程平均分统计")
    @PreAuthorize("@ss.hasAnyPerm('score:stat','score:input','score:audit')")
    @GetMapping("/stat/course")
    public Result<List<Map<String, Object>>> courseStats(@RequestParam(required = false) Long classId,
                                                         @RequestParam(required = false) Long semesterId) {
        return Result.success(scoreService.courseStats(classId, semesterId));
    }

    @Operation(summary = "全校成绩等级分布")
    @PreAuthorize("@ss.hasAnyPerm('score:stat','score:audit')")
    @GetMapping("/stat/level")
    public Result<List<Map<String, Object>>> levelDistribution() {
        return Result.success(scoreService.levelDistribution());
    }

    @Operation(summary = "班级学生成绩排名")
    @PreAuthorize("@ss.hasAnyPerm('score:stat','score:input','score:audit')")
    @GetMapping("/stat/rank")
    public Result<List<Map<String, Object>>> classRank(@RequestParam Long classId,
                                                       @RequestParam(required = false) Long semesterId,
                                                       @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(scoreService.classRank(classId, semesterId, limit));
    }

    @Operation(summary = "导出成绩")
    @OperLog(module = "成绩管理", operation = "导出成绩")
    @PreAuthorize("@ss.hasAnyPerm('score:audit','score:input')")
    @GetMapping("/export")
    public void export(ScoreQuery query, HttpServletResponse response) {
        query.setPageNum(1);
        query.setPageSize(500);
        var page = scoreService.page(query, DataScope.of(SecurityUtils.getLoginUser()));
        ExcelUtils.export(response, "成绩表", "成绩记录",
                new String[]{"学号", "姓名", "班级", "课程编号", "课程名称", "课程性质", "学分", "平时成绩", "期末成绩", "总评成绩", "绩点", "等级", "状态", "授课教师", "学期"},
                page.getRecords(),
                s -> new Object[]{
                        s.getStudentNo(), s.getStudentName(), s.getClassName(), s.getCourseCode(),
                        s.getCourseName(), s.getCourseType(), s.getCredit(), s.getUsualScore(),
                        s.getExamScore(), s.getTotalScore(), s.getGradePoint(), s.getLevel(),
                        statusText(s.getStatus()), s.getTeacherName(), s.getSemesterName()
                });
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "未录入";
            case 1 -> "已录入";
            case 2 -> "已发布";
            default -> "未知";
        };
    }
}
