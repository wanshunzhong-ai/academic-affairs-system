package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.dto.DataScope;
import com.aas.dto.query.StudentQuery;
import com.aas.entity.BaseClass;
import com.aas.entity.StuStudent;
import com.aas.entity.SysUser;
import com.aas.mapper.BaseClassMapper;
import com.aas.mapper.ScoreRecordMapper;
import com.aas.mapper.StuStudentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生服务
 */
@Service
@RequiredArgsConstructor
public class StuStudentService {

    private static final long STUDENT_ROLE_ID = 1L;

    private final StuStudentMapper studentMapper;
    private final BaseClassMapper classMapper;
    private final ScoreRecordMapper scoreMapper;
    private final SysUserService sysUserService;

    /**
     * 分页查询（含数据权限）
     */
    public PageResult<StuStudent> page(StudentQuery query, DataScope scope) {
        IPage<StuStudent> page = studentMapper.selectStudentPage(query.toPage(), query, scope);
        return PageResult.of(page);
    }

    public StuStudent detail(Long id) {
        StuStudent student = studentMapper.selectStudentDetail(id);
        if (student == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        return student;
    }

    /** 学生本人的档案 */
    public StuStudent myProfile(Long userId) {
        StuStudent student = studentMapper.selectByUserId(userId);
        if (student == null) {
            throw new BizException("未找到与当前账号关联的学生档案");
        }
        return student;
    }

    /**
     * 新增学生：同时创建登录账号
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(StuStudent student) {
        if (student.getStudentNo() == null || student.getStudentNo().isBlank()) {
            throw new BizException("学号不能为空");
        }
        Long dup = studentMapper.selectCount(new LambdaQueryWrapper<StuStudent>()
                .eq(StuStudent::getStudentNo, student.getStudentNo()));
        if (dup != null && dup > 0) {
            throw new BizException("学号【" + student.getStudentNo() + "】已存在");
        }
        fillRelation(student);
        if (student.getStatus() == null) {
            student.setStatus(1);
        }

        // 创建登录账号(账号=学号, 默认密码 123456)
        SysUser user = sysUserService.createAccount(student.getStudentNo(), student.getName(),
                "STUDENT", student.getGender(), student.getPhone(), student.getEmail(), STUDENT_ROLE_ID);
        student.setUserId(user.getId());

        studentMapper.insert(student);
        refreshClassCount(student.getClassId());
        return student.getId();
    }

    /**
     * 修改学生：同步账号信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(StuStudent student) {
        StuStudent exists = studentMapper.selectById(student.getId());
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        if (student.getStudentNo() != null && !student.getStudentNo().equals(exists.getStudentNo())) {
            Long dup = studentMapper.selectCount(new LambdaQueryWrapper<StuStudent>()
                    .eq(StuStudent::getStudentNo, student.getStudentNo())
                    .ne(StuStudent::getId, student.getId()));
            if (dup != null && dup > 0) {
                throw new BizException("学号【" + student.getStudentNo() + "】已存在");
            }
        }
        fillRelation(student);
        student.setUserId(exists.getUserId());
        studentMapper.updateById(student);
        sysUserService.updateAccount(exists.getUserId(), student.getName(), student.getGender(),
                student.getPhone(), student.getEmail());
        if (exists.getClassId() != null && !exists.getClassId().equals(student.getClassId())) {
            refreshClassCount(exists.getClassId());
        }
        refreshClassCount(student.getClassId());
    }

    /**
     * 删除学生：同时删除账号
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        StuStudent student = studentMapper.selectById(id);
        if (student == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        studentMapper.deleteById(id);
        sysUserService.deleteAccount(student.getUserId());
        refreshClassCount(student.getClassId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        ids.forEach(this::delete);
    }

    /** 修改学籍状态 */
    public void changeStatus(Long id, Integer status) {
        StuStudent student = studentMapper.selectById(id);
        if (student == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        StuStudent update = new StuStudent();
        update.setId(id);
        update.setStatus(status);
        studentMapper.updateById(update);
    }

    /** 学生成绩概览 */
    public Map<String, Object> summary(Long studentId) {
        Map<String, Object> summary = scoreMapper.selectStudentSummary(studentId);
        return summary == null ? new HashMap<>() : summary;
    }

    /** 班级学生列表 */
    public List<StuStudent> listByClass(Long classId) {
        return studentMapper.selectList(new LambdaQueryWrapper<StuStudent>()
                .eq(StuStudent::getClassId, classId)
                .eq(StuStudent::getStatus, 1)
                .orderByAsc(StuStudent::getStudentNo));
    }

    /** 全部在读学生(下拉用) */
    public List<StuStudent> listSimple(String keyword) {
        return studentMapper.selectList(new LambdaQueryWrapper<StuStudent>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(StuStudent::getStudentNo, keyword)
                        .or().like(StuStudent::getName, keyword))
                .eq(StuStudent::getStatus, 1)
                .orderByAsc(StuStudent::getStudentNo)
                .last("LIMIT 200"));
    }

    /** 批量导出用 */
    public List<StuStudent> listForExport(StudentQuery query, DataScope scope) {
        query.setPageNum(1);
        query.setPageSize(500);
        return studentMapper.selectStudentPage(query.toPage(), query, scope).getRecords();
    }

    /**
     * Excel 批量导入学生
     * 模板列: 学号* | 姓名* | 性别* | 班级编码* | 手机号 | 身份证号 | 出生日期 | 政治面貌 | 宿舍 | 监护人 | 监护人电话 | 家庭住址
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(org.springframework.web.multipart.MultipartFile file) {
        List<java.util.Map<Integer, String>> rows = com.aas.util.ExcelUtils.read(file);
        if (rows == null || rows.isEmpty()) {
            throw new BizException("文件中没有可导入的数据");
        }
        int success = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            java.util.Map<Integer, String> row = rows.get(i);
            int lineNo = i + 2;
            try {
                String studentNo = com.aas.util.ExcelUtils.cell(row, 0);
                String name = com.aas.util.ExcelUtils.cell(row, 1);
                String genderText = com.aas.util.ExcelUtils.cell(row, 2);
                String classCode = com.aas.util.ExcelUtils.cell(row, 3);
                if (studentNo == null || studentNo.isBlank()) {
                    errors.add("第" + lineNo + "行：学号不能为空");
                    continue;
                }
                if (name == null || name.isBlank()) {
                    errors.add("第" + lineNo + "行：姓名不能为空");
                    continue;
                }
                if (classCode == null || classCode.isBlank()) {
                    errors.add("第" + lineNo + "行：班级编码不能为空");
                    continue;
                }
                BaseClass clazz = classMapper.selectOne(new LambdaQueryWrapper<BaseClass>()
                        .eq(BaseClass::getClassCode, classCode).last("LIMIT 1"));
                if (clazz == null) {
                    errors.add("第" + lineNo + "行：班级编码【" + classCode + "】不存在");
                    continue;
                }
                Long exists = studentMapper.selectCount(new LambdaQueryWrapper<StuStudent>()
                        .eq(StuStudent::getStudentNo, studentNo));
                if (exists != null && exists > 0) {
                    errors.add("第" + lineNo + "行：学号【" + studentNo + "】已存在");
                    continue;
                }
                StuStudent student = new StuStudent();
                student.setStudentNo(studentNo);
                student.setName(name);
                student.setGender("女".equals(genderText) ? 2 : 1);
                student.setClassId(clazz.getId());
                student.setPhone(com.aas.util.ExcelUtils.cell(row, 4));
                student.setIdCard(com.aas.util.ExcelUtils.cell(row, 5));
                String birth = com.aas.util.ExcelUtils.cell(row, 6);
                if (birth != null && !birth.isBlank()) {
                    student.setBirthDate(LocalDate.parse(birth.substring(0, 10)));
                }
                student.setPoliticalStatus(com.aas.util.ExcelUtils.cell(row, 7));
                student.setDormitory(com.aas.util.ExcelUtils.cell(row, 8));
                student.setGuardianName(com.aas.util.ExcelUtils.cell(row, 9));
                student.setGuardianPhone(com.aas.util.ExcelUtils.cell(row, 10));
                student.setAddress(com.aas.util.ExcelUtils.cell(row, 11));
                student.setEnrollmentDate(LocalDate.of(clazz.getEnrollmentYear() == null
                        ? LocalDate.now().getYear() : clazz.getEnrollmentYear(), 9, 1));
                student.setStatus(1);
                create(student);
                success++;
            } catch (Exception e) {
                errors.add("第" + lineNo + "行：" + e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("success", success);
        result.put("fail", rows.size() - success);
        result.put("errors", errors);
        return result;
    }

    private void fillRelation(StuStudent student) {
        if (student.getClassId() == null) {
            throw new BizException("请选择所属班级");
        }
        BaseClass clazz = classMapper.selectById(student.getClassId());
        if (clazz == null) {
            throw new BizException("所选班级不存在");
        }
        student.setMajorId(clazz.getMajorId());
    }

    private void refreshClassCount(Long classId) {
        if (classId == null) {
            return;
        }
        Long count = studentMapper.selectCount(new LambdaQueryWrapper<StuStudent>()
                .eq(StuStudent::getClassId, classId));
        BaseClass update = new BaseClass();
        update.setId(classId);
        update.setStudentCount(count == null ? 0 : count.intValue());
        classMapper.updateById(update);
    }

    /** 批量统计各班级人数 */
    public List<Map<String, Object>> classDistribution() {
        List<BaseClass> classes = classMapper.selectList(new LambdaQueryWrapper<BaseClass>()
                .orderByAsc(BaseClass::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (BaseClass clazz : classes) {
            Long count = studentMapper.selectCount(new LambdaQueryWrapper<StuStudent>()
                    .eq(StuStudent::getClassId, clazz.getId()));
            Map<String, Object> row = new HashMap<>();
            row.put("className", clazz.getClassName());
            row.put("count", count == null ? 0 : count);
            result.add(row);
        }
        return result;
    }

    /** 校验学分是否达标 */
    public boolean creditEnough(Long studentId, BigDecimal required) {
        Map<String, Object> summary = summary(studentId);
        Object earned = summary.get("earnedCredit");
        if (earned == null) {
            return false;
        }
        return new BigDecimal(earned.toString()).compareTo(required) >= 0;
    }
}
