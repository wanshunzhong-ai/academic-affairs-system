package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.dto.query.CommonQuery;
import com.aas.entity.BaseClassroom;
import com.aas.mapper.BaseClassroomMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 教室服务
 */
@Service
@RequiredArgsConstructor
public class BaseClassroomService {

    private final BaseClassroomMapper classroomMapper;

    public PageResult<BaseClassroom> page(CommonQuery query) {
        LambdaQueryWrapper<BaseClassroom> wrapper = new LambdaQueryWrapper<BaseClassroom>()
                .and(query.getKeyword() != null && !query.getKeyword().isBlank(), w -> w
                        .like(BaseClassroom::getRoomName, query.getKeyword())
                        .or().like(BaseClassroom::getRoomCode, query.getKeyword())
                        .or().like(BaseClassroom::getBuilding, query.getKeyword()))
                .eq(query.getRoomType() != null && !query.getRoomType().isBlank(),
                        BaseClassroom::getRoomType, query.getRoomType())
                .eq(query.getStatus() != null, BaseClassroom::getStatus, query.getStatus())
                .orderByAsc(BaseClassroom::getId);
        Page<BaseClassroom> page = query.toPage();
        return PageResult.of(classroomMapper.selectPage(page, wrapper));
    }

    public List<BaseClassroom> listAll() {
        return classroomMapper.selectList(new LambdaQueryWrapper<BaseClassroom>()
                .eq(BaseClassroom::getStatus, 1)
                .orderByAsc(BaseClassroom::getId));
    }

    public Long create(BaseClassroom classroom) {
        checkCodeUnique(classroom.getRoomCode(), null);
        if (classroom.getStatus() == null) {
            classroom.setStatus(1);
        }
        if (classroom.getCapacity() == null) {
            classroom.setCapacity(60);
        }
        classroomMapper.insert(classroom);
        return classroom.getId();
    }

    public void update(BaseClassroom classroom) {
        if (classroomMapper.selectById(classroom.getId()) == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        checkCodeUnique(classroom.getRoomCode(), classroom.getId());
        classroomMapper.updateById(classroom);
    }

    public void delete(Long id) {
        classroomMapper.deleteById(id);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            throw new BizException("教室编号不能为空");
        }
        Long count = classroomMapper.selectCount(new LambdaQueryWrapper<BaseClassroom>()
                .eq(BaseClassroom::getRoomCode, code)
                .ne(excludeId != null, BaseClassroom::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("教室编号【" + code + "】已存在");
        }
    }
}
