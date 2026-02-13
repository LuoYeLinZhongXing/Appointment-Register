package com.luoye.controller;

import com.luoye.Result;
import com.luoye.annotation.OperationLogger;
import com.luoye.constant.MessageConstant;
import com.luoye.dto.dept.DeptCreateDTO;
import com.luoye.dto.dept.DeptPageQueryDTO;
import com.luoye.dto.dept.DeptStatusUpdateDTO;
import com.luoye.dto.dept.DeptUpdateDTO;
import com.luoye.entity.Dept;
import com.luoye.service.DeptService;
import com.luoye.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dept")
@Tag(name = "科室管理", description = "科室相关操作接口")
public class DeptController {

    @Autowired
    private DeptService deptService;

    /**
     * 创建科室
     * @param deptCreateDTO 科室创建数据传输对象
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(summary = "创建科室", description = "创建新的科室信息")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @OperationLogger(operationType = "CREATE", targetType = "DEPT")
    public Result<String> create(@RequestBody DeptCreateDTO deptCreateDTO) {
        deptService.create(deptCreateDTO);
        return Result.success(MessageConstant.CREATE_SUCCESS);
    }

    /**
     * 分页查询科室信息
     * @param deptPageQueryDTO 科室分页查询数据传输对象
     * @return 分页查询结果
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询科室", description = "根据ID、名称、类型、状态进行分页查询，支持按创建时间排序")
    @ApiResponse(responseCode = "200", description = "查询成功", 
                content = @Content(schema = @Schema(implementation = PageResult.class)))
    public Result<PageResult<Dept>> deptPageQuery(@RequestBody DeptPageQueryDTO deptPageQueryDTO) {
        Map<String, Object> pageResult = deptService.pageQuery(deptPageQueryDTO);
        PageResult<com.luoye.entity.Dept> deptPageResult = new PageResult<>();
        deptPageResult.setRecords((List<com.luoye.entity.Dept>) pageResult.get("depts"));
        deptPageResult.setTotal((Long) pageResult.get("total"));
        deptPageResult.setPages((Integer) pageResult.get("pages"));
        deptPageResult.setCurrent((Integer) pageResult.get("current"));
        deptPageResult.setSize((Integer) pageResult.get("size"));
        return Result.success(deptPageResult);
    }
    
    /**
     * 更新科室信息
     * @param deptUpdateDTO 科室更新数据传输对象
     * @return 更新结果
     */
    @PutMapping("/update")
    @Operation(summary = "更新科室信息", description = "根据科室ID更新科室信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @OperationLogger(operationType = "UPDATE", targetType = "DEPT")
    public Result<String> update(@RequestBody DeptUpdateDTO deptUpdateDTO) {
        deptService.update(deptUpdateDTO);
        return Result.success(MessageConstant.UPDATE_SUCCESS);
    }

    /**
     * 删除科室
     * @param id 科室ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "物理删除科室", description = "根据科室ID物理删除科室信息")
    @Parameter(name = "id", description = "科室ID", required = true)
    @ApiResponse(responseCode = "200", description = "删除成功")
    @OperationLogger(operationType = "DELETE", targetType = "DEPT")
    public Result<String> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.success(MessageConstant.DELETE_SUCCESS);
    }

    /**
     * 更新科室状态
     * @param statusUpdateDTO 状态更新数据传输对象
     */
    @PutMapping("/status")
    @Operation(summary = "更新科室状态", description = "批量更新科室状态")
    @ApiResponse(responseCode = "200", description = "状态更新成功")
    @OperationLogger(operationType = "UPDATE", targetType = "DEPT")
    public Result<String> updateStatus( @RequestBody DeptStatusUpdateDTO statusUpdateDTO) {
        deptService.updateStatus(statusUpdateDTO);
        return Result.success(MessageConstant.STATUS_UPDATE_SUCCESS);
    }
    
    /**
     * 根据ID查询科室
     * @param id 科室ID
     * @return 科室实体
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询科室", description = "根据科室ID获取科室详细信息")
    @Parameter(name = "id", description = "科室ID", required = true)
    @ApiResponse(responseCode = "200", description = "查询成功", 
                content = @Content(schema = @Schema(implementation = Dept.class)))
    public Result<Dept> getById(@PathVariable Long id) {
        com.luoye.entity.Dept dept = deptService.getById(id);
        return Result.success(dept);
    }

    /**
     * 获取所有启用的科室
     * @return 科室列表
     */
    @GetMapping("/getAllEnabled")
    @Operation(summary = "获取所有启用的科室", description = "获取所有启用状态的科室列表")
    @ApiResponse(responseCode = "200", description = "查询成功", 
                content = @Content(schema = @Schema(implementation = Dept.class)))
    public Result<List<Dept>> getAllEnabledDepts() {
        List<Dept> depts = deptService.getAllEnabledDepts();
        return Result.success(depts);
    }

    /**
     * 获取所有科室的ID和名称
     * @return 科室ID和名称列表
     */
    @GetMapping("/getAllIdAndName")
    @Operation(summary = "获取所有科室ID和名称", description = "获取所有科室的ID和名称信息")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<Map<String, Object>>> getAllDeptIdAndName() {
        List<Map<String, Object>> deptInfoList = deptService.getAllDeptIdAndName();
        return Result.success(deptInfoList);
    }
}
