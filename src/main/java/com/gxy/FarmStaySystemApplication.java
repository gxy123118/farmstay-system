package com.gxy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gxy.mapper")
public class FarmStaySystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarmStaySystemApplication.class, args);
    }

}
