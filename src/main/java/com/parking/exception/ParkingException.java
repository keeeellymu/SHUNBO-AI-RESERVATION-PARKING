package com.parking.exception;

public class ParkingException extends RuntimeException {
    private int code;
    
    public ParkingException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public ParkingException(String message) {
        super(message);
        this.code = 500;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
}