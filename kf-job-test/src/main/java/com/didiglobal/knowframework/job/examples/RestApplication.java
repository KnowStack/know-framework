package com.didiglobal.knowframework.job.examples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@ComponentScan({"com.didiglobal.knowframework.job", "com.didiglobal.knowframework"})
@SpringBootApplication
@EnableSwagger2
public class RestApplication {
    public static void main(String[] args) {
        SpringApplication.run(RestApplication.class);
    }
}