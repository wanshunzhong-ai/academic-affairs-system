package com.aas.service;

import com.aas.common.PageResult;
import com.aas.dto.PageQuery;
import com.aas.entity.SysOperLog;
import com.aas.mapper.SysOperLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务
 */
@Service
@RequiredArgsConstructor
public class SysOperLogService {

    private final SysOperLogMapper operLogMapper;

    public PageResult<SysOperLog> page(String keyword, Integer status, String module,
                                       String startDate, String endDate, PageQuery pageQuery) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<SysOperLog>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysOperLog::getUsername, keyword)
                        .or().like(SysOperLog::getRealName, keyword)
                        .or().like(SysOperLog::getOperation, keyword))
                .eq(status != null, SysOperLog::getStatus, status)
                .eq(module != null && !module.isBlank(), SysOperLog::getModule, module)
                .ge(startDate != null && !startDate.isBlank(), SysOperLog::getCreateTime,
                        startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate).atStartOfDay() : null)
                .le(endDate != null && !endDate.isBlank(), SysOperLog::getCreateTime,
                        endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate).atTime(23, 59, 59) : null)
                .orderByDesc(SysOperLog::getId);
        Page<SysOperLog> page = pageQuery.toPage();
        return PageResult.of(operLogMapper.selectPage(page, wrapper));
    }

    public void deleteBatch(List<Long> ids) {
        operLogMapper.deleteByIds(ids);
    }

    public void clear() {
        operLogMapper.delete(new LambdaQueryWrapper<>());
    }

    /** 清理 N 天前的日志 */
    public int cleanBefore(int days) {
        return operLogMapper.delete(new LambdaQueryWrapper<SysOperLog>()
                .lt(SysOperLog::getCreateTime, LocalDateTime.now().minusDays(days)));
    }

    public List<String> modules() {
        return operLogMapper.selectObjs(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysOperLog>()
                        .select("DISTINCT module").isNotNull("module"))
                .stream().map(String::valueOf).toList();
    }
}
