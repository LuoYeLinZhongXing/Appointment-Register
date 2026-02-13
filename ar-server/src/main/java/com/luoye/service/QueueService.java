package com.luoye.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoye.dto.doctor.DoctorCallDTO;
import com.luoye.entity.Order;
import com.luoye.entity.Queue;

import java.util.List;

public interface QueueService extends IService<Queue> {

    /**
     * 初始化医生队列
     * @param doctorId 医生ID
     * @return 是否成功
     */
    boolean initializeDoctorQueueInRedis(Long doctorId);

    /**
     * 批量初始化所有在职医生的队列
     * @return 初始化成功的医生数量
     */
    int initializeAllDoctorsQueue();

    /**
     * 从Redis获取医生队列
     * @param doctorId 医生ID
     * @return 队列列表
     */
    List<Queue> getDoctorQueueFromRedis(Long doctorId);

    /**
     * 将队列数据同步到Redis
     * @param doctorId 医生ID
     * @param queueList 队列列表
     * @return 是否同步成功
     */
    boolean syncQueueToRedis(Long doctorId, List<Queue> queueList);

    /**
     * 支付成功后处理排队逻辑
     * @param order 订单信息
     */
    void handleQueueAfterPayment(Order order);

    /**
     * 从排队队列中删除指定订单的排队记录
     * @param orderId 订单ID
     */
    void removeFromQueue(Long orderId);

    /**
     * 获取医生的下一个排队号码
     * @param doctorId 医生ID
     * @return 下一个排队号码
     */
    Integer getNextQueueNumber(Long doctorId);

    /**
     * 获取患者在队列中的位置
     * @param patientId 患者ID
     * @param doctorId 医生ID
     * @return 队列位置（从1开始），-1表示不在队列中
     */
    Integer getPatientQueuePosition(Long patientId, Long doctorId);

    /**
     * 叫号
     * @param doctorCallDTO 医生叫号信息
     * @return 叫号结果
     */
    Queue callNextPatient(DoctorCallDTO doctorCallDTO);



    /**
     * 处理患者未到号
     * @param queueId 队列ID
     */
    void handleMissedPatient( Long queueId);

    /**
     * 开始治疗
     * @param queueId 队列ID
     */
    void startTreatment(Long queueId);

    /**
     * 医生完成就诊
     * @param queueId 队列ID
     */
    void completeTreatment(Long queueId);

}
