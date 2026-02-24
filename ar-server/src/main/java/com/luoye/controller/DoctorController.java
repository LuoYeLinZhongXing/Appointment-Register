package com.luoye.controller;

import com.luoye.Result;
import com.luoye.annotation.OperationLogger;
import com.luoye.constant.MessageConstant;
import com.luoye.context.BaseContext;
import com.luoye.dto.doctor.*;
import com.luoye.entity.Doctor;
import com.luoye.entity.Queue;
import com.luoye.service.DoctorService;
import com.luoye.service.QueueService;
import com.luoye.vo.PageResult;
import com.luoye.vo.QueueDetailVO;
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
@RequestMapping("/doctor")
@Tag(name = "医生管理", description = "医生相关操作接口")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private QueueService queueService;

    /**
     * 医生注册
     * @param doctorRegisterDTO 医生注册数据传输对象
     * @return 注册结果
     */
    @PostMapping("/register")
    @Operation(summary = "医生注册", description = "新医生注册账户")
    @ApiResponse(responseCode = "200", description = "注册成功")
    @OperationLogger(operationType = "CREATE", targetType = "DOCTOR")
    public Result<String> register(@RequestBody DoctorRegisterDTO doctorRegisterDTO) {
            doctorService.register(doctorRegisterDTO);
            return Result.success(MessageConstant.REGISTER_SUCCESS);
    }

    /**
     * 医生登录
     * @param doctorLoginDTO 医生登录数据传输对象
     * @return 登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "医生登录", description = "通过手机号和密码进行医生登录")
    @ApiResponse(responseCode = "200", description = "登录成功")
    public Result<Map<String, Object>> login(@RequestBody DoctorLoginDTO doctorLoginDTO) {
            Map<String, Object> result = doctorService.login(doctorLoginDTO);
            return Result.success(result);
    }

    /**
     * 医生忘记密码
     * @param forgotPasswordDTO 忘记密码数据传输对象
     * @return 重置结果
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "医生忘记密码", description = "通过手机号重置医生密码")
    @ApiResponse(responseCode = "200", description = "密码重置成功")
    public Result<String> forgotPassword(@RequestBody DoctorForgotPasswordDTO forgotPasswordDTO) {
        doctorService.forgotPassword(forgotPasswordDTO.getPhone(), forgotPasswordDTO.getNewPassword());
        return Result.success(MessageConstant.PASSWORD_RESET_SUCCESS);
    }

    /**
     * 分页查询医生信息
     * @param doctorPageQueryDTO 医生分页查询数据传输对象
     * @return 分页查询结果
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询医生", description = "根据ID、姓名、职位、状态、科室进行分页查询，支持按创建时间排序")
    @ApiResponse(responseCode = "200", description = "查询成功",
                content = @Content(schema = @Schema(implementation = PageResult.class)))
    public Result<PageResult<Doctor>> doctorPageQuery(@RequestBody DoctorPageQueryDTO doctorPageQueryDTO) {
            System.out.println( doctorPageQueryDTO);
            PageResult<Doctor> pageResult =doctorService.pageQuery(doctorPageQueryDTO);
            System.out.println( pageResult);
            return Result.success(pageResult);
    }

    /**
     * 更新医生信息
     * @param doctorUpdateDTO 医生更新数据传输对象
     * @return 更新结果
     */
    @PutMapping("/update")
    @Operation(summary = "更新医生信息", description = "根据医生ID更新医生信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @OperationLogger(operationType = "UPDATE", targetType = "DOCTOR")
    public Result<String> update(@RequestBody DoctorUpdateDTO doctorUpdateDTO) {
            doctorService.update(doctorUpdateDTO);
            return Result.success(MessageConstant.UPDATE_SUCCESS);
    }

    /**
     * 删除医生
     * @param id 医生ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除医生", description = "根据医生ID删除医生")
    @Parameter(name = "id", description = "医生ID", required = true)
    @ApiResponse(responseCode = "200", description = "删除成功")
    @OperationLogger(operationType = "DELETE", targetType = "DOCTOR")
    public Result<String> delete(@PathVariable Long id) {
            doctorService.delete(id);
            return Result.success(MessageConstant.DELETE_SUCCESS);
    }

    /**
     * 更新医生状态
     * @param doctorStatusUpdateDTO 医生状态更新数据传输对象
     * @return 更新状态结果
     */
    @PatchMapping("/status")
    @Operation(summary = "更新医生状态", description = "根据医生ID更新医生状态，如离职等")
    @ApiResponse(responseCode = "200", description = "状态更新成功")
    @OperationLogger(operationType = "UPDATE", targetType = "DOCTOR")
    public Result<String> updateStatus(@RequestBody DoctorStatusUpdateDTO doctorStatusUpdateDTO) {
        doctorService.updateStatus(doctorStatusUpdateDTO);
        return Result.success(MessageConstant.STATUS_UPDATE_SUCCESS);
    }

    /**
     * 根据ID查询医生
     * @param id 医生ID
     * @return 医生实体
     */
    @GetMapping("/getById/{id}")
    @Operation(summary = "根据ID查询医生", description = "根据医生ID获取医生详细信息")
    @Parameter(name = "id", description = "医生ID", required = true)
    @ApiResponse(responseCode = "200", description = "查询成功",
                content = @Content(schema = @Schema(implementation = Doctor.class)))
    public Result<Doctor> getById(@PathVariable Long id) {
            Doctor doctor = doctorService.getById(id);
            return Result.success(doctor);
    }

    /**
     * 根据科室ID查询医生
     * @param deptId 科室ID
     * @return 医生列表
     */
    @GetMapping("/getDoctorsByDeptId/{deptId}")
    @Operation(summary = "根据科室ID查询医生", description = "根据科室ID获取医生列表")
    @Parameter(name = "deptId", description = "科室ID", required = true)
    @ApiResponse(responseCode = "200", description = "查询成功",
                content = @Content(schema = @Schema(implementation = Doctor.class)))
    public Result<List<Doctor>> getDoctorsByDeptId(@PathVariable Long deptId) {
            List<Doctor> doctors = doctorService.getDoctorsByDeptId(deptId);
            return Result.success(doctors);
    }

    /**
     * 查询当前排队队列
     * @return 队列列表
     */
    @GetMapping("/current-queue")
    @Operation(summary = "查询当前排队队列", description = "医生查询自己当前的排队队列信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = Queue.class)))
    public Result<List<QueueDetailVO>> getCurrentQueue() {
        // 验证医生身份
        String currentUserType = BaseContext.getCurrentIdentity();
        Long currentUserId = BaseContext.getCurrentId();

        if (!"DOCTOR".equals(currentUserType)) {
            throw new RuntimeException(MessageConstant.NO_PERMISSION);
        }

        // 获取当前医生的排队队列
        List<QueueDetailVO> queueList = queueService.getDoctorQueueDetailsFromRedis(currentUserId);
        return Result.success(queueList);
    }

    /**
     * 医生叫号
     * @return 叫号结果
     */
    @PostMapping("/call-next")
    @Operation(summary = "医生叫号", description = "医生呼叫下一个患者")
    @ApiResponse(responseCode = "200", description = "叫号成功",
                content = @Content(schema = @Schema(implementation = Queue.class)))
    @OperationLogger(operationType = "UPDATE", targetType = "QUEUE")
    public Result<Queue> callNextPatient() {
        Queue calledPatient = queueService.callNextPatient();
        return Result.success(calledPatient);
    }

    /**
     * 医生开始治疗
     * @param queueId 队列ID
     * @return 开始治疗结果
     */
    @PostMapping("/start-treatment/{queueId}")
    @Operation(summary = "开始治疗", description = "医生手动开始为患者治疗")
    @Parameter(name = "queueId", description = "队列ID", required = true)
    @ApiResponse(responseCode = "200", description = "开始就诊成功")
    @OperationLogger(operationType = "UPDATE", targetType = "QUEUE")
    public Result<String> startTreatment(@PathVariable Long queueId) {
        // 验证医生身份
        String currentUserType = BaseContext.getCurrentIdentity();
        Long currentUserId = BaseContext.getCurrentId();

        if (!"DOCTOR".equals(currentUserType)) {
            throw new RuntimeException(MessageConstant.NO_PERMISSION);
        }

        // 查询队列记录验证权限
        Queue queue = queueService.getById(queueId);
        if (queue == null) {
            throw new RuntimeException("未找到排队记录");
        }

        // 验证是否是当前医生的患者
        if (!queue.getDoctorId().equals(currentUserId)) {
            throw new RuntimeException(MessageConstant.NO_PERMISSION);
        }

        queueService.startTreatment(queueId);
        return Result.success("开始就诊成功");
    }

    /**
     * 医生完成就诊
     * @param queueId 队列ID
     * @return 处理结果
     */
    @PostMapping("/complete-treatment/{queueId}")
    @Operation(summary = "完成就诊", description = "医生完成当前患者的就诊")
    @Parameter(name = "queueId", description = "队列ID", required = true)
    @ApiResponse(responseCode = "200", description = "完成就诊成功")
    @OperationLogger(operationType = "UPDATE", targetType = "QUEUE")
    public Result<String> completeTreatment(@PathVariable Long queueId) {
        // 验证医生身份
        String currentUserType = BaseContext.getCurrentIdentity();
        Long currentUserId = BaseContext.getCurrentId();

        if (!"DOCTOR".equals(currentUserType)) {
            throw new RuntimeException(MessageConstant.NO_PERMISSION);
        }

        // 查询队列记录验证权限
        Queue queue = queueService.getById(queueId);
        if (queue == null) {
            throw new RuntimeException("未找到排队记录");
        }

        // 验证是否是当前医生的患者
        if (!queue.getDoctorId().equals(currentUserId)) {
            throw new RuntimeException(MessageConstant.NO_PERMISSION);
        }

        queueService.completeTreatment(queueId);
        return Result.success("完成就诊成功");
    }

    /**
     * 处理过号患者
     * @param queueId 队列ID
     * @return 处理结果
     */
    @PostMapping("/handle-missed/{queueId}")
    @Operation(summary = "处理过号患者", description = "医生手动过号处理")
    @Parameter(name = "queueId", description = "队列ID", required = true)
    @ApiResponse(responseCode = "200", description = "过号处理成功")
    @OperationLogger(operationType = "UPDATE", targetType = "QUEUE")
    public Result<String> handleMissedPatient(@PathVariable Long queueId) {
        // 验证医生身份
        String currentUserType = BaseContext.getCurrentIdentity();

        if (!"DOCTOR".equals(currentUserType)) {
            throw new RuntimeException(MessageConstant.NO_PERMISSION);
        }

        // 处理过号逻辑
        queueService.handleMissedPatient(queueId);

        return Result.success("过号处理成功");
    }
}
