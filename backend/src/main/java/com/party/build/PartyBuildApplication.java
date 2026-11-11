package com.party.build;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.party.build.**.mapper")
@EnableScheduling
public class PartyBuildApplication {

    public static void main(String[] args) {
        SpringApplication.run(PartyBuildApplication.class, args);
    }
}