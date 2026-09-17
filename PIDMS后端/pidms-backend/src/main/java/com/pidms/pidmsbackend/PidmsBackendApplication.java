package com.pidms.pidmsbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.pidms.pidmsbackend.mapper")

public class PidmsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PidmsBackendApplication.class, args);
    }

}
