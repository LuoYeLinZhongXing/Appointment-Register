package com.luoye.dto.dept;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 科室创建数据传输对象
 */
@Data
public class DeptCreateDTO {

    /**
     * 科室名称
     */
    @NotBlank(message = "科室名称不能为空")
    private String name;

    /**
     * 科室类型 :0.临床 1.医技
     */
    @NotNull(message = "科室类型不能为空")
    private Integer type;

    /**
     * 科室位置
     */
    private String location;

    /**
     * 科室状态: 0.停用 1.启用
     */
    private Integer status = 1;

    /**
     * 简介
     */
    private String description;

    /**
     * 负责人id
     */
    private Long directorId;
}
