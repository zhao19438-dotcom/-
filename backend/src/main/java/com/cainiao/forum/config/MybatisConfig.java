package com.cainiao.forum.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;



@Configuration

@MapperScan("com.cainiao.forum.dao")
// MyBatis 配置类：告诉 Spring 去 dao 包下寻找 Mapper 接口。
public class MybatisConfig {
}

