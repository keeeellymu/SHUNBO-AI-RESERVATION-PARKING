package com.parking;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
@MapperScan("com.parking.dao")
@ComponentScan("com.parking")
public class ParkingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParkingApplication.class, args);
    }
}