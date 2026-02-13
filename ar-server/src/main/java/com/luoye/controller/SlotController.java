package com.luoye.controller;

import com.luoye.Result;
import com.luoye.annotation.OperationLogger;
import com.luoye.dto.slot.SlotPageQueryDTO;
import com.luoye.dto.slot.SlotReleaseDTO;
import com.luoye.entity.Slot;
import com.luoye.exception.BaseException;
import com.luoye.service.SlotService;
import com.luoye.vo.PageResult;
import com.luoye.vo.SlotVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 号源控制层
 */
@RestController
@RequestMapping("/slot")
@Slf4j
@Tag(name = "号源管理", description = "号源相关操作接口")
public class SlotController {

    @Autowired
    private SlotService slotService;

    /**
     * 放出号源
     * @param slotReleaseDTO 号源信息
     * @return 操作结果
     */
    @PostMapping("/release")
    @OperationLogger(operationType = "CREATE", targetType = "SLOT")
    @Operation(summary = "放出号源", description = "放出指定医生指定日期的号源")
    @ApiResponse(responseCode = "200", description = "号源放出成功")
    public Result<String> releaseSlot(@RequestBody SlotReleaseDTO slotReleaseDTO) {
        Slot slot = slotService.releaseSlot(slotReleaseDTO);
        if (slot != null) {
            return Result.success("号源放出成功");
        } else {
            return Result.error("号源放出失败");
        }
    }

    /**
     * 根据医生ID和日期查询号源
     * @param doctorId 医生ID
     * @param scheduleDate 出诊日期
     * @return 号源列表
     */
    @GetMapping("/byDoctorAndDate")
    @Operation(summary = "根据医生ID和日期查询号源", description = "根据医生ID和日期查询号源")
    @Parameter(name = "doctorId", description = "医生ID", required = true)
    @Parameter(name = "scheduleDate", description = "出诊日期(格式: yyyy-MM-dd)", required = true)
    @ApiResponse(responseCode = "200", description = "查询成功",
                content = @Content(schema = @Schema(implementation = SlotVO.class)))
    public Result<List<SlotVO>> getSlotsByDoctorAndDate(@RequestParam Long doctorId,
                                                        @RequestParam String scheduleDate) {
        List<SlotVO> slots = slotService.getSlotsByDoctorAndDate(doctorId, java.time.LocalDate.parse(scheduleDate));
        return Result.success(slots);
    }

    /**
     * 分页查询号源
     * @param slotPageQueryDTO 查询条件
     * @return 分页查询结果
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询号源", description = "根据出诊日期、时间段、号源状态等条件分页查询号源")
    @ApiResponse(responseCode = "200", description = "查询成功",
                content = @Content(schema = @Schema(implementation = PageResult.class)))
    public Result<PageResult<Slot>> pageQueryByConditions(@RequestBody SlotPageQueryDTO slotPageQueryDTO) {
        PageResult<Slot> pageResult = slotService.pageQueryByConditions(slotPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据ID获取单个号源
     * @param slotId 号源ID
     * @return 号源信息
     */
    @GetMapping("/getById/{slotId}")
    @Operation(summary = "根据ID获取号源", description = "根据号源ID获取号源详细信息（支持缓存）")
    @Parameter(name = "slotId", description = "号源ID", required = true)
    @ApiResponse(responseCode = "200", description = "查询成功",
                content = @Content(schema = @Schema(implementation = Slot.class)))
    public Result<Slot> getSlotById(@PathVariable Long slotId) {
        Slot slot = slotService.getSlotById(slotId);
        if (slot != null) {
            return Result.success(slot);
        } else {
            return Result.error("号源不存在");
        }
    }

    /**
     * 删除号源
     * @param slotId 号源ID
     * @return 删除结果
     */
    @DeleteMapping("/deleteById/{slotId}")
    @OperationLogger(operationType = "DELETE", targetType = "SLOT")
    @Operation(summary = "删除号源", description = "删除号源并清理相关缓存")
    @Parameter(name = "slotId", description = "号源ID", required = true)
    @ApiResponse(responseCode = "200", description = "删除成功")
    public Result<String> deleteSlot(@PathVariable Long slotId) {
        boolean success = slotService.deleteSlot(slotId);
        if (success) {
            return Result.success("号源删除成功");
        } else {
            return Result.error("号源删除失败");
        }
    }
}
