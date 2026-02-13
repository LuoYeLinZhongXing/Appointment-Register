package com.luoye.controller;

import com.luoye.Result;
import com.luoye.annotation.OperationLogger;
import com.luoye.constant.MessageConstant;
import com.luoye.context.BaseContext;
import com.luoye.dto.admin.AdminForgotPasswordDTO;
import com.luoye.dto.admin.AdminLoginDTO;
import com.luoye.dto.admin.AdminRegisterDTO;
import com.luoye.dto.admin.AdminUpdateDTO;
import com.luoye.entity.Admin;
import com.luoye.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@Tag(name = "管理员管理", description = "管理员相关操作接口")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    /**
     * 管理员注册
     * @param adminRegisterDTO 管理员注册数据传输对象
     * @return 注册结果
     */
    @PostMapping("/register")
    @Operation(summary = "管理员注册", description = "通过手机号和密码进行管理员注册")
    @ApiResponse(responseCode = "200", description = "注册成功")
    @OperationLogger(operationType = "CREATE", targetType = "ADMIN")
    public Result<String> register(@RequestBody AdminRegisterDTO adminRegisterDTO) {
        // 移除try-catch，让GlobalExceptionHandler处理异常
        adminService.register(adminRegisterDTO);
        return Result.success("注册成功");
    }

    /**
     * 管理员登录
     * @param adminLoginDTO 管理员登录数据传输对象
     * @return 登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "管理员使用手机号和密码登录")
    @ApiResponse(responseCode = "200", description = "登录成功")
    @OperationLogger(operationType = "LOGIN", targetType = "ADMIN")
    public Result<Map<String, Object>> login(@RequestBody AdminLoginDTO adminLoginDTO) {
        logger.info("收到管理员登录请求: {}", adminLoginDTO.getPhone());

        if(adminLoginDTO.getPhone() == null || adminLoginDTO.getPassword() == null) {
            logger.warn("登录参数不完整，手机号: {}, 密码: {}",
                    adminLoginDTO.getPhone(), adminLoginDTO.getPassword() != null ? "已提供" : "未提供");
            throw new RuntimeException(MessageConstant.PHONE_PASSWORD_NOT_NULL);
        }

        logger.info("开始处理登录逻辑...");
        Map<String, Object> loginResult = adminService.login(adminLoginDTO);
        logger.info("登录逻辑处理完成，准备返回结果");
        return Result.success(loginResult);
    }

    /**
     * 管理员忘记密码
     * @param forgotPasswordDTO 忘记密码数据传输对象
     * @return 重置结果
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "管理员忘记密码", description = "通过手机号重置管理员密码")
    @ApiResponse(responseCode = "200", description = "密码重置成功")
    public Result<String> forgotPassword(@RequestBody AdminForgotPasswordDTO forgotPasswordDTO) {
        adminService.forgotPassword(forgotPasswordDTO.getPhone(), forgotPasswordDTO.getNewPassword());
        return Result.success(MessageConstant.PASSWORD_RESET_SUCCESS);
    }

    /**
     * 根据ID查询管理员信息
     * @param id 管理员ID
     * @return 管理员信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询管理员信息", description = "根据管理员ID获取详细信息")
    @Parameter(name = "id", description = "管理员ID", required = true)
    @ApiResponse(responseCode = "200", description = "查询成功", 
                content = @Content(schema = @Schema(implementation = Admin.class)))
    public Result<Admin> getById(@PathVariable Long id) {
        logger.info("查询管理员信息，ID: {}", id);
        Admin admin = adminService.getById(id);
        logger.info("成功查询到管理员信息，ID: {}", id);
        return Result.success(admin);
    }

    /**
     * 管理员修改自身信息
     * @param adminUpdateDTO 管理员更新数据传输对象
     * @return 更新结果
     */
    @PutMapping("/update")
    @Operation(summary = "管理员修改自身信息", description = "管理员修改自己的个人信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @OperationLogger(operationType = "UPDATE", targetType = "ADMIN")
    public Result<String> update(@RequestBody AdminUpdateDTO adminUpdateDTO) {
        // 移除try-catch，让GlobalExceptionHandler处理异常
        adminService.update(adminUpdateDTO);
        return Result.success("更新成功");
    }

    /**
     * 获取当前登录管理员信息
     * @return 管理员信息
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前登录管理员信息", description = "获取当前登录的管理员详细信息")
    @ApiResponse(responseCode = "200", description = "查询成功", 
                content = @Content(schema = @Schema(implementation = Admin.class)))
    public Result<Admin> getCurrentAdmin() {
        Long currentAdminId = BaseContext.getCurrentId();
        if (currentAdminId == null) {
            throw new RuntimeException("请先登录");
        }

        Admin admin = adminService.getById(currentAdminId);
        // 清除敏感信息
        admin.setPassword(null);
        return Result.success(admin);
    }

}
