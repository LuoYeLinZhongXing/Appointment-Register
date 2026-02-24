package com.luoye.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoye.dto.RegisterDTO;
import com.luoye.dto.order.OrderCancelDTO;
import com.luoye.dto.order.OrderPageQueryDTO;
import com.luoye.vo.OrderDetailVO;
import com.luoye.entity.Order;
import com.luoye.vo.PageResult;

public interface OrderService extends IService<Order> {

    /**
     * 挂号并创建订单
     * @param registerDTO 挂号信息
     * @return 订单ID
     */
    Long registerAndCreateOrder(RegisterDTO registerDTO);

    /**
     * 根据订单ID获取订单信息
     * @param orderId 订单ID
     * @return 订单信息
     */
    Order getOrderById(Long orderId);

    /**
     * 根据订单ID获取订单详细信息
     * @param orderId 订单ID
     * @return 订单详细信息
     */
    OrderDetailVO getOrderDetailById(Long orderId);

    /**
     * 支付订单
     * @param orderId 订单ID
     * @return 是否支付成功
     */
    boolean payOrder(Long orderId);

    /**
     * 取消订单
     * @param  orderCancelDTO 取消订单信息
     * @return 是否取消成功
     */
    boolean cancelOrder(OrderCancelDTO orderCancelDTO);

    /**
     * 分页查询订单信息
     * @param orderPageQueryDTO 查询条件
     * @return 订单信息
     */
    PageResult<OrderDetailVO> pageQueryOrders(OrderPageQueryDTO orderPageQueryDTO);
}
