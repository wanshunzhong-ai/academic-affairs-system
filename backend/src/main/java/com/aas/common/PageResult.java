package com.aas.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页返回结果
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> records = new ArrayList<>();

    /** 总记录数 */
    private Long total = 0L;

    /** 当前页码 */
    private Long current = 1L;

    /** 每页条数 */
    private Long size = 10L;

    /** 总页数 */
    private Long pages = 0L;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Long current, Long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = size == null || size == 0 ? 0L : (total + size - 1) / size;
    }

    /**
     * 由 MyBatis-Plus 分页对象转换
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 由 MyBatis-Plus 分页对象转换并映射元素类型
     */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> mapper) {
        List<T> list = page.getRecords().stream().map(mapper).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>();
    }
}
