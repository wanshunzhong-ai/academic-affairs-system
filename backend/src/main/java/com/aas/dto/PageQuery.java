package com.aas.dto;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询基础参数
 */
@Data
public class PageQuery {

    @Schema(description = "页码, 从1开始", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "排序字段(数据库列名)")
    private String orderByColumn;

    @Schema(description = "排序方向 asc/desc")
    private String isAsc = "desc";

    /**
     * 转换为 MyBatis-Plus 分页对象
     */
    public <T> Page<T> toPage() {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 500);
        Page<T> page = new Page<>(num, size);
        if (orderByColumn != null && !orderByColumn.isBlank()) {
            boolean asc = "asc".equalsIgnoreCase(isAsc);
            page.addOrder(asc ? OrderItem.asc(camelToUnderline(orderByColumn))
                    : OrderItem.desc(camelToUnderline(orderByColumn)));
        }
        return page;
    }

    private static String camelToUnderline(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
