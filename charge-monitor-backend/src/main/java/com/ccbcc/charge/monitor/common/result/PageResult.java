package com.ccbcc.charge.monitor.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 通用分页返回对象
 *
 * @param <T> 分页记录类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页数据
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long pageNo;

    /**
     * 每页数量
     */
    private Long pageSize;

    /**
     * 总页数
     */
    private Long pages;

    public static <T> PageResult<T> of(List<T> records,
                                       Long total,
                                       Long pageNo,
                                       Long pageSize) {
        long pages = pageSize == null || pageSize == 0
                ? 0
                : (total + pageSize - 1) / pageSize;

        return new PageResult<>(records, total, pageNo, pageSize, pages);
    }
}