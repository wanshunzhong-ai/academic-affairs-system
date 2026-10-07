package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.dto.query.TeacherQuery;
import com.aas.entity.BaseClass;
import com.aas.entity.SysUser;
import com.aas.entity.TeaTeacher;
import com.aas.mapper.BaseClassMapper;
import com.aas.mapper.TeaTeacherMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师服务
 */
@Service
@RequiredArgsConstructor
public class TeaTeacherService {

    private static final long HEAD_TEACHER_ROLE_ID = 2L;

    private final TeaTeacherMapper teacherMapper;
    private final BaseClassMapper classMapper;
    private final SysUserService sysUserService;

    public PageResult<TeaTeacher> page(TeacherQuery query) {
        Page<TeaTeacher> page = query.toPage();
        return PageResult.of(teacherMapper.selectTeacherPage(page, query));
    }

    public TeaTeacher detail(Long id) {
        TeaTeacher teacher = teacherMapper.selectTeacherDetail(id);
        if (teacher == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        teacher.setHeadClassIds(teacherMapper.selectManageClassIds(id));
        return teacher;
    }

    public TeaTeacher myProfile(Long userId) {
        TeaTeacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            throw new BizException("未找到与当前账号关联的教师档案");
        }
        teacher.setHeadClassIds(teacherMapper.selectManageClassIds(teacher.getId()));
        List<String> classNames = teacherMapper.selectManageClassNames(teacher.getId());
        teacher.setHeadClassNames(classNames == null ? null : String.join("、", classNames));
        return teacher;
    }

    /** 全部教师(下拉用) */
    public List<TeaTeacher> listAll(Long deptId) {
        return teacherMapper.selectList(new LambdaQueryWrapper<TeaTeacher>()
                .eq(deptId != null, TeaTeacher::getDeptId, deptId)
                .eq(TeaTeacher::getStatus, 1)
                .orderByAsc(TeaTeacher::getId));
    }

    /** 班主任列表(下拉用) */
    public List<TeaTeacher> listHeadTeachers() {
        return teacherMapper.selectList(new LambdaQueryWrapper<TeaTeacher>()
                .eq(TeaTeacher::getIsHeadTeacher, 1)
                .eq(TeaTeacher::getStatus, 1)
                .orderByAsc(TeaTeacher::getId));
    }

    /**
     * 新增教师：若为班主任则创建登录账号
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(TeaTeacher teacher) {
        if (teacher.getTeacherNo() == null || teacher.getTeacherNo().isBlank()) {
            throw new BizException("工号不能为空");
        }
        Long dup = teacherMapper.selectCount(new LambdaQueryWrapper<TeaTeacher>()
                .eq(TeaTeacher::getTeacherNo, teacher.getTeacherNo()));
        if (dup != null && dup > 0) {
            throw new BizException("工号【" + teacher.getTeacherNo() + "】已存在");
        }
        if (teacher.getStatus() == null) {
            teacher.setStatus(1);
        }
        if (teacher.getIsHeadTeacher() == null) {
            teacher.setIsHeadTeacher(0);
        }
        teacher.setUserId(null);
        teacherMapper.insert(teacher);

        if (teacher.getIsHeadTeacher() == 1) {
            SysUser user = sysUserService.createAccount(teacher.getTeacherNo(), teacher.getName(),
                    "TEACHER", teacher.getGender(), teacher.getPhone(), teacher.getEmail(), HEAD_TEACHER_ROLE_ID);
            teacher.setUserId(user.getId());
            teacherMapper.updateById(teacher);
        }
        return teacher.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(TeaTeacher teacher) {
        TeaTeacher exists = teacherMapper.selectById(teacher.getId());
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        if (teacher.getTeacherNo() != null && !teacher.getTeacherNo().equals(exists.getTeacherNo())) {
            Long dup = teacherMapper.selectCount(new LambdaQueryWrapper<TeaTeacher>()
                    .eq(TeaTeacher::getTeacherNo, teacher.getTeacherNo())
                    .ne(TeaTeacher::getId, teacher.getId()));
            if (dup != null && dup > 0) {
                throw new BizException("工号【" + teacher.getTeacherNo() + "】已存在");
            }
        }
        teacher.setUserId(exists.getUserId());

        // 升为班主任且无账号 → 补建账号
        if (teacher.getIsHeadTeacher() != null && teacher.getIsHeadTeacher() == 1 && exists.getUserId() == null) {
            SysUser user = sysUserService.createAccount(teacher.getTeacherNo(), teacher.getName(),
                    "TEACHER", teacher.getGender(), teacher.getPhone(), teacher.getEmail(), HEAD_TEACHER_ROLE_ID);
            teacher.setUserId(user.getId());
        }
        teacherMapper.updateById(teacher);
        sysUserService.updateAccount(exists.getUserId(), teacher.getName(), teacher.getGender(),
                teacher.getPhone(), teacher.getEmail());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        TeaTeacher teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        Long classCount = classMapper.selectCount(new LambdaQueryWrapper<BaseClass>()
                .eq(BaseClass::getHeadTeacherId, id));
        if (classCount != null && classCount > 0) {
            throw new BizException("该教师仍担任班主任，请先解除班级关联");
        }
        teacherMapper.deleteById(id);
        sysUserService.deleteAccount(teacher.getUserId());
    }

    /** 为教师分配管理班级 */
    @Transactional(rollbackFor = Exception.class)
    public void assignClasses(Long teacherId, List<Long> classIds) {
        TeaTeacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        // 解除该教师原有班级
        classMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BaseClass>()
                .eq(BaseClass::getHeadTeacherId, teacherId)
                .set(BaseClass::getHeadTeacherId, null));
        if (classIds != null && !classIds.isEmpty()) {
            for (Long classId : classIds) {
                BaseClass update = new BaseClass();
                update.setId(classId);
                update.setHeadTeacherId(teacherId);
                classMapper.updateById(update);
            }
        }
        TeaTeacher flag = new TeaTeacher();
        flag.setId(teacherId);
        flag.setIsHeadTeacher(classIds != null && !classIds.isEmpty() ? 1 : 0);
        teacherMapper.updateById(flag);
    }

    public void changeStatus(Long id, Integer status) {
        TeaTeacher teacher = new TeaTeacher();
        teacher.setId(id);
        teacher.setStatus(status);
        teacherMapper.updateById(teacher);
    }

    /** 教师工作量统计 */
    public Map<String, Object> workload(Long teacherId) {
        Map<String, Object> map = new HashMap<>();
        map.put("offeringCount", 0);
        map.put("studentCount", 0);
        map.put("classCount", classMapper.selectCount(new LambdaQueryWrapper<BaseClass>()
                .eq(BaseClass::getHeadTeacherId, teacherId)));
        return map;
    }
}
