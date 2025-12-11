package com.parking.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultDTO {
    private boolean success;
    private String message;
    private Object data;
    private int code;
    
    public static ResultDTO success(Object data) {
        return new ResultDTO(true, "操作成功", data, 200);
    }
    
    public static ResultDTO fail(String message) {
        return new ResultDTO(false, message, null, 500);
    }
    
    public static ResultDTO fail(String message, int code) {
        return new ResultDTO(false, message, null, code);
    }
}