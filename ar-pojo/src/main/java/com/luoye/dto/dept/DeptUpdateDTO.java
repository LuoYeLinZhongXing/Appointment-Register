package com.luoye.dto.dept;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.luoye.constant.MessageConstant;

@Data
public class DeptUpdateDTO {
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
     * 科室位置
     */
    private String location;

    /**
     * 科室状态: 0.停用 1.启用
     */
    private Integer status;

    /**
     * 简介
     */
    private String description;

    /**
     * 负责人id
     */
    private Long directorId;
}
