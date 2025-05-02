package com.axconstantino.reservationsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BookingManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingManagementSystemApplication.class, args);
    }

}