package com.cainiao.forum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// Spring Boot 项目的启动入口，运行 main 方法后会启动内置 Web 服务器。
public class ForumApplication {

	public static void main(String[] args) {
		// 将当前类交给 Spring Boot，用来扫描组件、读取配置并启动应用。
		SpringApplication.run(ForumApplication.class, args);
	}

}

