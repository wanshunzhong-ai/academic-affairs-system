package com.aas.dto.query;

import com.aas.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "公告查询条件")
public class NoticeQuery extends PageQuery {

    private String keyword;

    @Schema(description = "通知/公告/教务/紧急")
    private String noticeType;

    @Schema(description = "ALL/CLASS/ROLE")
    private String scope;

    @Schema(description = "0草稿 1已发布 2已下架")
    private Integer status;
}
