package com.parking.exception;

import com.parking.model.dto.ResultDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// 移除Spring Security相关导入
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(ParkingException.class)
    @ResponseBody
    public ResponseEntity<ResultDTO> handleParkingException(ParkingException ex, WebRequest request) {
        ResultDTO result = ResultDTO.fail(ex.getMessage(), ex.getCode());
        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理访问拒绝异常 (403错误)
     * 注意：暂时注释掉，因为移除了Spring Security依赖
     */
    /*
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ResponseEntity<ResultDTO> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        // 记录访问拒绝日志
        System.err.println("访问拒绝: " + request.getDescription(false) + " - " + ex.getMessage());
        
        // 返回友好的403错误响应
        ResultDTO result = ResultDTO.fail("权限不足，无法访问该资源");
        return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
    }
    */
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ResultDTO> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        
        String errorMessage = "参数验证失败：" + errors.toString();
        ResultDTO result = ResultDTO.fail(errorMessage);
        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ResultDTO> handleAllExceptions(Exception ex, WebRequest request) {
        // 记录详细的异常信息，包括类型、消息和部分堆栈
        System.err.println("系统内部错误 - 异常类型: " + ex.getClass().getName());
        System.err.println("系统内部错误 - 异常消息: " + ex.getMessage());
        ex.printStackTrace();
        
        // 返回更详细的错误信息
        ResultDTO result = ResultDTO.fail("系统内部错误: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}