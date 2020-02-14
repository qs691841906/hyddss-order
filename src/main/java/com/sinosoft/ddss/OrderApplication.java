package com.sinosoft.ddss;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

//Spring Boot 应用的标识
@SpringBootApplication

@MapperScan("com.sinosoft.ddss.dao")
@EnableEurekaClient//表示可以作为服务向注册中心注册
public class OrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}
}
