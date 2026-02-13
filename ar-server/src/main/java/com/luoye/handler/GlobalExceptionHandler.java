package com.luoye.handler;


import com.luoye.Result;
import com.luoye.constant.MessageConstant;
import com.luoye.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    /**
     * 捕获404路径未找到异常
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.error("请求路径未找到：{}", ex.getRequestURL());
        String message = "请求的路径 '" + ex.getRequestURL() + "' 不存在，请检查URL是否正确";
        return Result.error(message);
    }

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }


    /**
     * 捕获参数类型不匹配异常
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.error("参数类型不匹配异常：{}", ex.getMessage(), ex);

        String paramName = ex.getName();
        String receivedValue = ex.getValue().toString();

        // 根据参数名给出友好的提示
        String friendlyMessage;
        if ("id".equals(paramName)) {
            friendlyMessage = MessageConstant.ID_FORMAT_ERROR;
        } else if (paramName.toLowerCase().contains("number") ||
                paramName.toLowerCase().contains("count") ||
                paramName.toLowerCase().contains("size")) {
            friendlyMessage = MessageConstant.NUMBER_FORMAT_ERROR;
        } else {
            friendlyMessage = MessageConstant.PARAMETER_TYPE_MISMATCH;
        }

        log.warn("参数转换错误 - 参数名: {}, 输入值: {}, 错误信息: {}",
                paramName, receivedValue, friendlyMessage);

        return Result.error(friendlyMessage);
    }

    /**
     * 捕获运行时异常
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException ex) {
        log.error("运行时异常：{}", ex.getMessage(), ex);
        // 如果是预定义的消息常量，直接返回对应消息
        if (ex.getMessage() != null) {
            // 可以在这里添加更多的消息映射逻辑
            return Result.error(ex.getMessage());
        }
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }

    /**
     * 捕获所有异常
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception ex) {
        log.error("未处理的异常：{}", ex.getMessage(), ex);
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
    /**
     * 捕获sql异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        //Duplicate entry 'zhangsan' for key 'employee.idx_username'
        String message = ex.getMessage();
        if(message.contains("Duplicate entry")){
            String[] split = message.split(" ");
            String username = split[2];
            String msg = username + MessageConstant.ALREADY_EXISTS;
            return Result.error(msg);
        }else{
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }

    }

}
