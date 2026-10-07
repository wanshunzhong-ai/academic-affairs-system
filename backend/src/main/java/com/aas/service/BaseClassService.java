package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.dto.query.CommonQuery;
import com.aas.entity.BaseClass;
import com.aas.entity.BaseMajor;
import com.aas.entity.StuStudent;
import com.aas.entity.TeaTeacher;
import com.aas.mapper.BaseClassMapper;
import com.aas.mapper.BaseMajorMapper;
import com.aas.mapper.StuStudentMapper;
import com.aas.mapper.TeaTeacherMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 班级服务
 */
@Service
@RequiredArgsConstructor
public class BaseClassService {

    private final BaseClassMapper classMapper;
    private final BaseMajorMapper majorMapper;
    private final StuStudentMapper studentMapper;
    private final TeaTeacherMapper teacherMapper;

    public PageResult<BaseClass> page(CommonQuery query) {
        LambdaQueryWrapper<BaseClass> wrapper = new LambdaQueryWrapper<BaseClass>()
                .and(query.getKeyword() != null && !query.getKeyword().isBlank(), w -> w
                        .like(BaseClass::getClassName, query.getKeyword())
                        .or().like(BaseClass::getClassCode, query.getKeyword()))
                .eq(query.getMajorId() != null, BaseClass::getMajorId, query.getMajorId())
                .eq(query.getGrade() != null && !query.getGrade().isBlank(), BaseClass::getGrade, query.getGrade())
                .eq(query.getStatus() != null, BaseClass::getStatus, query.getStatus());
        if (query.getDeptId() != null) {
            List<Long> majorIds = majorMapper.selectList(new LambdaQueryWrapper<BaseMajor>()
                            .eq(BaseMajor::getDeptId, query.getDeptId()))
                    .stream().map(BaseMajor::getId).toList();
            if (majorIds.isEmpty()) {
                return PageResult.empty();
            }
            wrapper.in(BaseClass::getMajorId, majorIds);
        }
        wrapper.orderByAsc(BaseClass::getGrade).orderByAsc(BaseClass::getId);
        Page<BaseClass> page = query.toPage();
        PageResult<BaseClass> result = PageResult.of(classMapper.selectPage(page, wrapper));
        fillExtra(result.getRecords());
        return result;
    }

    public List<BaseClass> listAll() {
        List<BaseClass> list = classMapper.selectList(new LambdaQueryWrapper<BaseClass>()
                .eq(BaseClass::getStatus, 1)
                .orderByAsc(BaseClass::getGrade).orderByAsc(BaseClass::getId));
        fillExtra(list);
        return list;
    }

    /** 教师所管理的班级 */
    public List<BaseClass> listByHeadTeacher(Long teacherId) {
        List<BaseClass> list = classMapper.selectList(new LambdaQueryWrapper<BaseClass>()
                .eq(BaseClass::getHeadTeacherId, teacherId)
                .orderByAsc(BaseClass::getId));
        fillExtra(list);
        return list;
    }

    public BaseClass detail(Long id) {
        BaseClass clazz = classMapper.selectById(id);
        if (clazz == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        fillExtra(List.of(clazz));
        return clazz;
    }

    public Long create(BaseClass clazz) {
        checkCodeUnique(clazz.getClassCode(), null);
        if (clazz.getStatus() == null) {
            clazz.setStatus(1);
        }
        if (clazz.getStudentCount() == null) {
            clazz.setStudentCount(0);
        }
        classMapper.insert(clazz);
        return clazz.getId();
    }

    public void update(BaseClass clazz) {
        if (classMapper.selectById(clazz.getId()) == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        checkCodeUnique(clazz.getClassCode(), clazz.getId());
        classMapper.updateById(clazz);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long studentCount = studentMapper.selectCount(new LambdaQueryWrapper<StuStudent>()
                .eq(StuStudent::getClassId, id));
        if (studentCount != null && studentCount > 0) {
            throw new BizException("该班级下存在学生，无法删除");
        }
        classMapper.deleteById(id);
    }

    /** 重新统计班级人数 */
    public void refreshStudentCount(Long classId) {
        Long count = studentMapper.selectCount(new LambdaQueryWrapper<StuStudent>()
                .eq(StuStudent::getClassId, classId));
        BaseClass update = new BaseClass();
        update.setId(classId);
        update.setStudentCount(count == null ? 0 : count.intValue());
        classMapper.updateById(update);
    }

    private void fillExtra(List<BaseClass> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, BaseMajor> majorMap = majorMapper.selectList(null).stream()
                .collect(Collectors.toMap(BaseMajor::getId, Function.identity(), (a, b) -> a));
        Map<Long, TeaTeacher> teacherMap = teacherMapper.selectList(null).stream()
                .collect(Collectors.toMap(TeaTeacher::getId, Function.identity(), (a, b) -> a));
        for (BaseClass clazz : list) {
            BaseMajor major = majorMap.get(clazz.getMajorId());
            if (major != null) {
                clazz.setMajorName(major.getMajorName());
                clazz.setDeptId(major.getDeptId());
            }
            TeaTeacher teacher = clazz.getHeadTeacherId() == null ? null : teacherMap.get(clazz.getHeadTeacherId());
            if (teacher != null) {
                clazz.setHeadTeacherName(teacher.getName());
                clazz.setHeadTeacherPhone(teacher.getPhone());
            }
        }
    }

    private void checkCodeUnique(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            throw new BizException("班级编码不能为空");
        }
        Long count = classMapper.selectCount(new LambdaQueryWrapper<BaseClass>()
                .eq(BaseClass::getClassCode, code)
                .ne(excludeId != null, BaseClass::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("班级编码【" + code + "】已存在");
        }
    }
}
