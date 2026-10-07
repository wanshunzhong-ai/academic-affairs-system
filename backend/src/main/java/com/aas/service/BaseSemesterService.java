package com.aas.service;

import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.entity.BaseSemester;
import com.aas.mapper.BaseSemesterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学期服务
 */
@Service
@RequiredArgsConstructor
public class BaseSemesterService {

    private final BaseSemesterMapper semesterMapper;

    public List<BaseSemester> listAll() {
        return semesterMapper.selectList(new LambdaQueryWrapper<BaseSemester>()
                .orderByDesc(BaseSemester::getIsCurrent)
                .orderByDesc(BaseSemester::getId));
    }

    /** 当前学期 */
    public BaseSemester current() {
        BaseSemester semester = semesterMapper.selectOne(new LambdaQueryWrapper<BaseSemester>()
                .eq(BaseSemester::getIsCurrent, 1).last("LIMIT 1"));
        if (semester == null) {
            semester = semesterMapper.selectOne(new LambdaQueryWrapper<BaseSemester>()
                    .orderByDesc(BaseSemester::getId).last("LIMIT 1"));
        }
        return semester;
    }

    public BaseSemester detail(Long id) {
        BaseSemester semester = semesterMapper.selectById(id);
        if (semester == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        return semester;
    }

    public Long create(BaseSemester semester) {
        if (semester.getSemesterName() == null || semester.getSemesterName().isBlank()) {
            throw new BizException("学期名称不能为空");
        }
        if (semester.getIsCurrent() == null) {
            semester.setIsCurrent(0);
        }
        if (semester.getStatus() == null) {
            semester.setStatus(1);
        }
        if (semester.getIsCurrent() == 1) {
            clearCurrent();
        }
        semesterMapper.insert(semester);
        return semester.getId();
    }

    public void update(BaseSemester semester) {
        if (semesterMapper.selectById(semester.getId()) == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        if (semester.getIsCurrent() != null && semester.getIsCurrent() == 1) {
            clearCurrent();
        }
        semesterMapper.updateById(semester);
    }

    /** 设为当前学期 */
    @Transactional(rollbackFor = Exception.class)
    public void setCurrent(Long id) {
        BaseSemester semester = semesterMapper.selectById(id);
        if (semester == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        clearCurrent();
        semesterMapper.update(null, new LambdaUpdateWrapper<BaseSemester>()
                .eq(BaseSemester::getId, id)
                .set(BaseSemester::getIsCurrent, 1)
                .set(BaseSemester::getStatus, 1));
    }

    public void delete(Long id) {
        BaseSemester semester = semesterMapper.selectById(id);
        if (semester == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        if (semester.getIsCurrent() != null && semester.getIsCurrent() == 1) {
            throw new BizException("当前学期不允许删除");
        }
        Long offeringCount = semesterMapper.selectCount(null);
        semesterMapper.deleteById(id);
    }

    /** 是否处于选课时间窗口内 */
    public boolean inSelectWindow(Long semesterId) {
        BaseSemester semester = semesterId == null ? current() : semesterMapper.selectById(semesterId);
        if (semester == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (semester.getSelectStart() != null && now.isBefore(semester.getSelectStart())) {
            return false;
        }
        return semester.getSelectEnd() == null || !now.isAfter(semester.getSelectEnd());
    }

    private void clearCurrent() {
        semesterMapper.update(null, new LambdaUpdateWrapper<BaseSemester>()
                .set(BaseSemester::getIsCurrent, 0));
    }
}
