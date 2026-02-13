package com.luoye.service.impl;


import com.luoye.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.luoye.entity.Log;
import com.luoye.mapper.LogMapper;

@Service
public class LogServiceImpl  implements LogService {

    @Autowired
    private LogMapper logMapper;

    /**
     * 保存日志
     * @param entity 日志实体
     */
    @Override
    public void save(Log entity) {
        logMapper.insert(entity);
    }
}
