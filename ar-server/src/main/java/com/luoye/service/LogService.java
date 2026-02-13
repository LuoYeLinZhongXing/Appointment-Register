package com.luoye.service;

import com.luoye.entity.Log;

public interface LogService {
    /**
     * 保存日志
     * @param log 日志信息
     */
    void save(Log log);
}
