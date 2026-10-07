package com.aas.service;

import com.aas.common.PageResult;
import com.aas.common.ResultCode;
import com.aas.common.exception.BizException;
import com.aas.dto.query.CommonQuery;
import com.aas.entity.BaseDept;
import com.aas.entity.BaseMajor;
import com.aas.mapper.BaseDeptMapper;
import com.aas.mapper.BaseMajorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 院系服务
 */
@Service
@RequiredArgsConstructor
public class BaseDeptService {

    private final BaseDeptMapper deptMapper;
    private final BaseMajorMapper majorMapper;

    public PageResult<BaseDept> page(CommonQuery query) {
        LambdaQueryWrapper<BaseDept> wrapper = new LambdaQueryWrapper<BaseDept>()
                .and(query.getKeyword() != null && !query.getKeyword().isBlank(), w -> w
                        .like(BaseDept::getDeptName, query.getKeyword())
                        .or().like(BaseDept::getDeptCode, query.getKeyword())
                        .or().like(BaseDept::getDean, query.getKeyword()))
                .eq(query.getStatus() != null, BaseDept::getStatus, query.getStatus())
                .orderByAsc(BaseDept::getSort);
        Page<BaseDept> page = query.toPage();
        return PageResult.of(deptMapper.selectPage(page, wrapper));
    }

    public List<BaseDept> listAll() {
        return deptMapper.selectList(new LambdaQueryWrapper<BaseDept>()
                .eq(BaseDept::getStatus, 1)
                .orderByAsc(BaseDept::getSort));
    }

    public BaseDept detail(Long id) {
        BaseDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        return dept;
    }

    public Long create(BaseDept dept) {
        checkCodeUnique(dept.getDeptCode(), null);
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        if (dept.getSort() == null) {
            dept.setSort(0);
        }
        deptMapper.insert(dept);
        return dept.getId();
    }

    public void update(BaseDept dept) {
        if (deptMapper.selectById(dept.getId()) == null) {
            throw new BizException(ResultCode.DATA_NOT_EXIST);
        }
        checkCodeUnique(dept.getDeptCode(), dept.getId());
        deptMapper.updateById(dept);
    }

    public void delete(Long id) {
        Long majorCount = majorMapper.selectCount(new LambdaQueryWrapper<BaseMajor>()
                .eq(BaseMajor::getDeptId, id));
        if (majorCount != null && majorCount > 0) {
            throw new BizException("该院系下存在专业，无法删除");
        }
        deptMapper.deleteById(id);
    }

    public void changeStatus(Long id, Integer status) {
        BaseDept dept = new BaseDept();
        dept.setId(id);
        dept.setStatus(status);
        deptMapper.updateById(dept);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            throw new BizException("院系编码不能为空");
        }
        Long count = deptMapper.selectCount(new LambdaQueryWrapper<BaseDept>()
                .eq(BaseDept::getDeptCode, code)
                .ne(excludeId != null, BaseDept::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("院系编码【" + code + "】已存在");
        }
    }
}
