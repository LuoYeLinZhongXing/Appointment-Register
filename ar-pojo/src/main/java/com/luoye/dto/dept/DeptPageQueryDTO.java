package com.luoye.dto.dept;

import lombok.Data;

/**
 * 科室分页条件查询参数
 */
@Data
public class DeptPageQueryDTO {
    /**
     * 科室id
     */
    private Long id;

    /**
     * 科室名称
     */
    private String name;

    /**
     * 科室类型 :0.临床 1.医技
     */
    private Integer type;

    /**
     * 科室状态: 0.停用 1.启用
     */
    private Integer status;

    /**
     * 页码，默认为1
     */
    private Integer page = 1;

    /**
     * 每页大小，默认为10
     */
    private Integer pageSize = 10;

    /**
     * 排序字段
     */
    private String sortBy;

    /**
     * 排序方向，默认为降序(desc)
     */
    private String sortDir = "desc";
}
