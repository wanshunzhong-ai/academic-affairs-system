package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.dto.query.CommonQuery;
import com.aas.entity.BaseClass;
import com.aas.entity.BaseDept;
import com.aas.entity.BaseMajor;
import com.aas.mapper.BaseClassMapper;
import com.aas.mapper.BaseDeptMapper;
import com.aas.mapper.BaseMajorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 专业服务
 */
@Service
@RequiredArgsConstructor
public class BaseMajorService {

    private final BaseMajorMapper majorMapper;
    private final BaseDeptMapper deptMapper;
    private final BaseClassMapper classMapper;

    public PageResult<BaseMajor> page(CommonQuery query) {
        LambdaQueryWrapper<BaseMajor> wrapper = new LambdaQueryWrapper<BaseMajor>()
                .and(query.getKeyword() != null && !query.getKeyword().isBlank(), w -> w
                        .like(BaseMajor::getMajorName, query.getKeyword())
                        .or().like(BaseMajor::getMajorCode, query.getKeyword()))
                .eq(query.getDeptId() != null, BaseMajor::getDeptId, query.getDeptId())
                .eq(query.getStatus() != null, BaseMajor::getStatus, query.getStatus())
                .orderByAsc(BaseMajor::getId);
        Page<BaseMajor> page = query.toPage();
        PageResult<BaseMajor> result = PageResult.of(majorMapper.selectPage(page, wrapper));
        fillDeptName(result.getRecords());
        return result;
    }

    public List<BaseMajor> listAll(Long deptId) {
        List<BaseMajor> list = majorMapper.selectList(new LambdaQueryWrapper<BaseMajor>()
                .eq(deptId != null, BaseMajor::getDeptId, deptId)
                .eq(BaseMajor::getStatus, 1)
                .orderByAsc(BaseMajor::getId));
        fillDeptName(list);
        return list;
    }

    public BaseMajor detail(Long id) {
        BaseMajor major = majorMapper.selectById(id);
        if (major == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        fillDeptName(List.of(major));
        return major;
    }

    public Long create(BaseMajor major) {
        checkCodeUnique(major.getMajorCode(), null);
        if (major.getStatus() == null) {
            major.setStatus(1);
        }
        if (major.getDuration() == null) {
            major.setDuration(4);
        }
        majorMapper.insert(major);
        return major.getId();
    }

    public void update(BaseMajor major) {
        if (majorMapper.selectById(major.getId()) == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        checkCodeUnique(major.getMajorCode(), major.getId());
        majorMapper.updateById(major);
    }

    public void delete(Long id) {
        Long classCount = classMapper.selectCount(new LambdaQueryWrapper<BaseClass>()
                .eq(BaseClass::getMajorId, id));
        if (classCount != null && classCount > 0) {
            throw new BizException("该专业下存在班级，无法删除");
        }
        majorMapper.deleteById(id);
    }

    public void changeStatus(Long id, Integer status) {
        BaseMajor major = new BaseMajor();
        major.setId(id);
        major.setStatus(status);
        majorMapper.updateById(major);
    }

    private void fillDeptName(List<BaseMajor> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, String> deptMap = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(BaseDept::getId, BaseDept::getDeptName, (a, b) -> a));
        list.forEach(m -> m.setDeptName(deptMap.get(m.getDeptId())));
    }

    private void checkCodeUnique(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            throw new BizException("专业编码不能为空");
        }
        Long count = majorMapper.selectCount(new LambdaQueryWrapper<BaseMajor>()
                .eq(BaseMajor::getMajorCode, code)
                .ne(excludeId != null, BaseMajor::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("专业编码【" + code + "】已存在");
        }
    }
}
