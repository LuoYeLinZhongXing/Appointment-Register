package com.luoye.service.impl;


import com.luoye.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.luoye.entity.Log;
import com.luoye.mapper.LogMapper;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LogServiceImpl  implements LogService {

    @Autowired
    private LogMapper logMapper;

    /**
     * 保存日志
     * @param entity 日志实体
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void save(Log entity) {
        try{
            // 确保必要字段不为 null
            if (entity.getOperatorType() == null) {
                entity.setOperatorType("ANONYMOUS");
            }
            if (entity.getSuccessFlag() == null) {
                entity.setSuccessFlag(1);
            }

            logMapper.insert(entity);
            log.debug("日志保存成功：{}", entity.getOperationDetail());
        }catch (Exception e){
            // 日志记录失败不影响主业务，但需要打印详细错误
            log.error("日志保存失败：{}, 日志内容：{}", e.getMessage(), entity, e);
            // 重新抛出异常以便主流程知道日志保存失败
            throw e;
        }
    }
}
