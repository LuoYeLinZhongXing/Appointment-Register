package com.luoye.dto.patient;

import lombok.Data;

/**
 * 患者分页条件查询参数
 */
@Data
public class PatientQueryDTO {
    /**
     * 患者id
     */
    private Long id;

    /**
     * 患者姓名
     */
    private String name;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 性别: 0.男 1.女
     */
    private Integer gender;

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
