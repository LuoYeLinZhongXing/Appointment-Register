package com.luoye.dto.doctor;

import lombok.Data;

/**
 * 分页条件查询参数
 */
@Data
public class DoctorPageQueryDTO {
    /**
     * 医生id
     */
    private Long id;

    /**
     * 医生姓名
     */
    private String name;

    /**
     * 医生职位: 0.普通医生 1.科室主任
     */
    private Integer post;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 科室id
     */
    private Long deptId;

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
