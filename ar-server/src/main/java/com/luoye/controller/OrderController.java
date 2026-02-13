package com.luoye.controller;

import com.luoye.Result;
import com.luoye.annotation.OperationLogger;
import com.luoye.constant.MessageConstant;
import com.luoye.dto.RegisterDTO;
import com.luoye.dto.order.OrderCancelDTO;
import com.luoye.vo.OrderDetailVO;
import com.luoye.service.OrderService;
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
@RequestMapping("/order")
@Tag(name = "订单管理", description = "挂号订单相关操作接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 挂号
     * @param registerDTO 挂号信息
     * @return 订单ID
     */
    @PostMapping("register")
    @OperationLogger(operationType = "CREATE", targetType = "PATIENT")
    @Operation(summary = "患者挂号", description = "患者选择号源进行挂号并生成订单")
    @ApiResponse(responseCode = "200", description = "挂号成功",
                content = @Content(schema = @Schema(implementation = Long.class)))
    public Result<Long> register(@RequestBody RegisterDTO registerDTO){
        Long orderId = orderService.registerAndCreateOrder(registerDTO);
        return Result.success(MessageConstant.ORDER_REGISTER_SUCCESS,orderId);
    }

    /**
     * 查询订单信息
     * @param orderId 订单ID
     * @return 订单信息
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "查询订单信息", description = "查询指定订单信息")
    @Parameter(name = "orderId", description = "订单ID", required = true)
    @ApiResponse(responseCode = "200", description = "查询成功",
                content = @Content(schema = @Schema(implementation = OrderDetailVO.class)))
    public Result<OrderDetailVO> getOrderById(@PathVariable Long orderId){
        OrderDetailVO orderDetailVO = orderService.getOrderDetailById(orderId);
        return  Result.success(orderDetailVO);
    }

    /**
     * 支付订单
     * @param orderId 订单ID
     * @return 支付结果
     */
    @PostMapping("/pay/{orderId}")
    @OperationLogger(operationType = "UPDATE", targetType = "ORDER")
    @Operation(summary = "支付订单", description = "支付指定订单")
    @Parameter(name = "orderId", description = "订单ID", required = true)
    @ApiResponse(responseCode = "200", description = "支付成功")
    public Result<Boolean> payOrder(@PathVariable Long orderId){
        boolean result = orderService.payOrder(orderId);
        if(result){
            return Result.success(MessageConstant.ORDER_PAY_SUCCESS,true);
        }else{
            return Result.success(MessageConstant.ORDER_PAY_FAILURE,false);
        }
    }

    /**
     * 取消订单
     * @param  orderCancelDTO 取消订单信息
     * @return 取消结果
     */
    @PostMapping("/cancel")
    @OperationLogger(operationType = "UPDATE", targetType = "ORDER")
    @Operation(summary = "取消订单", description = "取消指定订单")
    @ApiResponse(responseCode = "200", description = "取消成功")
    public Result<String> cancelOrder(@RequestBody OrderCancelDTO orderCancelDTO){

        boolean result = orderService.cancelOrder(orderCancelDTO);
        if(result){
            return Result.success(MessageConstant.ORDER_CANCEL_SUCCESS);
        }else{
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }
}
