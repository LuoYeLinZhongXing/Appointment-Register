package com.luoye.controller;

import com.luoye.Result;
import com.luoye.annotation.OperationLogger;
import com.luoye.constant.MessageConstant;
import com.luoye.context.BaseContext;
import com.luoye.dto.patient.*;
import com.luoye.entity.Patient;
import com.luoye.service.PatientService;
import com.luoye.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/patient")
@Tag(name = "患者管理", description = "患者相关操作接口")
public class PatientController {
    @Autowired
    private PatientService patientService;

    @PostMapping("/register")
    @Operation(summary = "患者注册", description = "新患者注册账户")
    @ApiResponse(responseCode = "200", description = "注册成功")
    @OperationLogger(operationType = "CREATE", targetType = "PATIENT")
    public Result<String> register(@RequestBody PatientRegisterDTO patientRegisterDTO) {
        patientService.register(patientRegisterDTO);
        return Result.success(MessageConstant.REGISTER_SUCCESS);
    }

    @PostMapping("/login")
    @Operation(summary = "患者登录", description = "通过手机号和密码进行患者登录")
    @ApiResponse(responseCode = "200", description = "登录成功")
    @OperationLogger(operationType = "LOGIN", targetType = "PATIENT")
    public Result<Map<String, Object>> login(@RequestBody PatientLoginDTO patientLoginDTO) {
        if(patientLoginDTO.getPhone() == null || patientLoginDTO.getPassword() == null)
            throw new RuntimeException(MessageConstant.PHONE_PASSWORD_NOT_NULL);
        Map<String, Object> login = patientService.login(patientLoginDTO);
        return Result.success(login);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "忘记密码", description = "通过手机号重置患者密码")
    @ApiResponse(responseCode = "200", description = "密码重置成功")
    public Result<String> forgotPassword(@RequestBody PatientForgotPasswordDTO forgotPasswordDTO) {
        patientService.forgotPassword(forgotPasswordDTO.getPhone(), forgotPasswordDTO.getNewPassword());
        return Result.success(MessageConstant.PASSWORD_RESET_SUCCESS);
    }

    @PutMapping("/update")
    @Operation(summary = "更新患者信息", description = "根据患者ID更新患者信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @OperationLogger(operationType = "UPDATE", targetType = "PATIENT")
    public Result<String> update(@RequestBody PatientUpdateDTO patientUpdateDTO) {
        patientService.update(patientUpdateDTO);
        return Result.success(MessageConstant.UPDATE_SUCCESS);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除患者", description = "根据患者ID删除患者")
    @Parameter(name = "id", description = "患者ID", required = true)
    @ApiResponse(responseCode = "200", description = "删除成功")
    @OperationLogger(operationType = "DELETE", targetType = "PATIENT")
    public Result<String> delete(@PathVariable Long id) {
        patientService.delete(id);
        return Result.success(MessageConstant.DELETE_SUCCESS);
    }

    @GetMapping("/getById/{id}")
    @Operation(summary = "查询患者详情", description = "根据患者ID获取详细信息")
    @Parameter(name = "id", description = "患者ID", required = true)
    @ApiResponse(responseCode = "200", description = "查询成功",
                content = @Content(schema = @Schema(implementation = Patient.class)))
    public Result<Patient> getById(@PathVariable Long id) {
        Patient patient = patientService.getById(id);
        return Result.success(patient);
    }

    @DeleteMapping("/logout")
    @Operation(summary = "患者注销", description = "当前登录患者注销账户")
    @ApiResponse(responseCode = "200", description = "注销成功")
    @OperationLogger(operationType = "DELETE", targetType = "PATIENT")
    public Result<String> logout() {
        String currentUserType = BaseContext.getCurrentIdentity();
        if (!"PATIENT".equals(currentUserType)) {
            throw new RuntimeException(MessageConstant.NO_PERMISSION);
        }

        Long currentPatientId = BaseContext.getCurrentId();
        if (currentPatientId == null) {
            throw new RuntimeException(MessageConstant.USER_NOT_LOGIN);
        }

        patientService.delete(currentPatientId);
        return Result.success(MessageConstant.DELETE_SUCCESS);
    }

    /**
     * 分页查询患者
     * @param patientQueryDTO 查询条件
     * @return 分页结果
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询患者", description = "根据ID、姓名、手机号、性别进行分页查询，支持按创建时间排序")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = PageResult.class)))
    public Result<PageResult<Patient>> patientPageQuery(@RequestBody PatientQueryDTO patientQueryDTO) {
        PageResult<Patient> pageResult = patientService.pageQuery(patientQueryDTO);
        return Result.success(pageResult);
    }
}
