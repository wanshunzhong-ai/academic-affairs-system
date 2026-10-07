package com.aas.controller;

import com.aas.common.PageResult;
import com.aas.common.Result;
import com.aas.common.annotation.OperLog;
import com.aas.dto.DataScope;
import com.aas.dto.query.StudentQuery;
import com.aas.entity.StuStudent;
import com.aas.security.SecurityUtils;
import com.aas.service.StuStudentService;
import com.aas.util.ExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 学生管理
 */
@Tag(name = "11-学生管理")
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StuStudentController {

    private final StuStudentService studentService;

    @Operation(summary = "分页查询学生(自动应用数据权限)")
    @PreAuthorize("@ss.hasPerm('student:query')")
    @GetMapping("/page")
    public Result<PageResult<StuStudent>> page(StudentQuery query) {
        return Result.success(studentService.page(query, SecurityUtils.getLoginUser() == null
                ? new DataScope() : DataScope.of(SecurityUtils.getLoginUser())));
    }

    @Operation(summary = "学生详情")
    @PreAuthorize("@ss.hasPerm('student:query')")
    @GetMapping("/{id}")
    public Result<StuStudent> detail(@PathVariable Long id) {
        return Result.success(studentService.detail(id));
    }

    @Operation(summary = "我的学籍信息(学生本人)")
    @GetMapping("/my")
    public Result<StuStudent> my() {
        return Result.success(studentService.myProfile(SecurityUtils.getUserId()));
    }

    @Operation(summary = "我的成绩概览(学生本人)")
    @GetMapping("/my/summary")
    public Result<Map<String, Object>> mySummary() {
        return Result.success(studentService.summary(SecurityUtils.getStudentId()));
    }

    @Operation(summary = "班级学生名单")
    @PreAuthorize("@ss.hasPerm('student:query')")
    @GetMapping("/class/{classId}")
    public Result<List<StuStudent>> listByClass(@PathVariable Long classId) {
        return Result.success(studentService.listByClass(classId));
    }

    @Operation(summary = "学生下拉选项")
    @PreAuthorize("@ss.hasPerm('student:query')")
    @GetMapping("/options")
    public Result<List<StuStudent>> options(@RequestParam(required = false) String keyword) {
        return Result.success(studentService.listSimple(keyword));
    }

    @Operation(summary = "新增学生(自动创建登录账号)")
    @OperLog(module = "学生管理", operation = "新增学生")
    @PreAuthorize("@ss.hasPerm('student:add')")
    @PostMapping
    public Result<Long> create(@RequestBody StuStudent student) {
        return Result.success("新增成功，登录账号为学号，初始密码 123456", studentService.create(student));
    }

    @Operation(summary = "修改学生")
    @OperLog(module = "学生管理", operation = "修改学生")
    @PreAuthorize("@ss.hasPerm('student:edit')")
    @PutMapping
    public Result<Void> update(@RequestBody StuStudent student) {
        studentService.update(student);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除学生")
    @OperLog(module = "学生管理", operation = "删除学生")
    @PreAuthorize("@ss.hasPerm('student:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量删除学生")
    @OperLog(module = "学生管理", operation = "批量删除学生")
    @PreAuthorize("@ss.hasPerm('student:remove')")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        studentService.deleteBatch(ids);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "修改学籍状态")
    @OperLog(module = "学生管理", operation = "修改学籍状态")
    @PreAuthorize("@ss.hasPerm('student:edit')")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        studentService.changeStatus(id, status);
        return Result.success("操作成功", null);
    }

    @Operation(summary = "导出学生 Excel")
    @OperLog(module = "学生管理", operation = "导出学生")
    @PreAuthorize("@ss.hasPerm('student:export')")
    @GetMapping("/export")
    public void export(StudentQuery query, HttpServletResponse response) {
        DataScope scope = DataScope.of(SecurityUtils.getLoginUser());
        List<StuStudent> list = studentService.listForExport(query, scope);
        ExcelUtils.export(response, "学生信息表", "学生信息",
                new String[]{"学号", "姓名", "性别", "院系", "专业", "班级", "年级", "手机号", "身份证号",
                        "出生日期", "政治面貌", "入学日期", "宿舍", "监护人", "监护人电话", "学籍状态", "家庭住址"},
                list,
                s -> new Object[]{
                        s.getStudentNo(), s.getName(), s.getGender() != null && s.getGender() == 1 ? "男" : "女",
                        s.getDeptName(), s.getMajorName(), s.getClassName(), s.getGrade(), s.getPhone(), s.getIdCard(),
                        s.getBirthDate(), s.getPoliticalStatus(), s.getEnrollmentDate(), s.getDormitory(),
                        s.getGuardianName(), s.getGuardianPhone(), statusText(s.getStatus()), s.getAddress()
                });
    }

    @Operation(summary = "下载学生导入模板")
    @GetMapping("/template")
    public void template(HttpServletResponse response) {
        ExcelUtils.export(response, "学生导入模板", "学生信息",
                new String[]{"学号*", "姓名*", "性别*", "班级编码*", "手机号", "身份证号", "出生日期(yyyy-MM-dd)",
                        "政治面貌", "宿舍", "监护人", "监护人电话", "家庭住址"},
                List.<StuStudent>of(),
                s -> new Object[]{});
    }

    @Operation(summary = "批量导入学生")
    @OperLog(module = "学生管理", operation = "导入学生")
    @PreAuthorize("@ss.hasPerm('student:import')")
    @PostMapping("/import")
    public Result<Map<String, Object>> importStudents(@RequestParam("file") MultipartFile file) {
        return Result.success(studentService.importExcel(file));
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 1 -> "在读";
            case 2 -> "休学";
            case 3 -> "退学";
            case 4 -> "毕业";
            default -> "未知";
        };
    }
}
