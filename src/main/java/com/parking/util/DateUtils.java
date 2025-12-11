package com.parking.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {
    public static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_FORMAT);
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * 格式化日期时间
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime.format(DEFAULT_FORMATTER);
    }
    
    /**
     * 格式化日期时间，使用指定格式
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * 解析日期时间字符串
     */
    public static LocalDateTime parse(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr, ISO_FORMATTER);
    }
    
    /**
     * 解析日期时间字符串，使用指定格式
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * 判断是否为未来时间
     */
    public static boolean isFutureTime(LocalDateTime dateTime) {
        return dateTime.isAfter(LocalDateTime.now());
    }
    
    /**
     * 转换Date为LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    /**
     * 转换LocalDateTime为Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}