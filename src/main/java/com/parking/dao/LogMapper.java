package com.parking.dao;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

/**
 * 日志Mapper接口
 */
@Mapper
public interface LogMapper {
    
    /**
     * 查询错误日志
     * @param module 模块名称
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 错误日志列表
     */
    List<Map<String, Object>> selectErrorLogs(String module, int offset, int limit);
    
    /**
     * 统计错误日志数量
     * @param module 模块名称
     * @return 错误日志数量
     */
    int countErrorLogs(String module);
    
    /**
     * 插入日志记录
     * @param logData 日志数据
     * @return 插入行数
     */
    int insertLog(Map<String, Object> logData);
}