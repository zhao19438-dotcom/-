package com.cainiao.forum.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;



@Configuration
// Web MVC 拦截器配置类，集中配置登录校验与后台操作审计。
public class AppInterceptorConfigurer implements WebMvcConfigurer {


    @Resource
    private LoginInterceptor loginInterceptor;

    @Resource
    private AuditLogInterceptor auditLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 审计拦截器只作用于后台接口：新增后台接口会自动被审计，无需改 Controller。
        registry.addInterceptor(auditLogInterceptor)
                .addPathPatterns("/admin/**");

        // 默认拦截所有请求，再把登录页、注册接口、静态资源和接口文档排除掉。
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/sign-in.html")
                .excludePathPatterns("/sign-up.html")
                .excludePathPatterns("/user/login")
                .excludePathPatterns("/user/register")
                // 验证码与密码找回：用户此时还没登录，必须放行。
                .excludePathPatterns("/user/captcha")
                .excludePathPatterns("/user/securityQuestion")
                .excludePathPatterns("/user/resetPwd")
                // 公开只读查询接口放行，支持未登录访客正常浏览论坛首页与帖子
                .excludePathPatterns("/board/topList")
                .excludePathPatterns("/board/available")
                .excludePathPatterns("/board/getById")
                .excludePathPatterns("/article/getAllByBoardId")
                .excludePathPatterns("/article/details")
                .excludePathPatterns("/article/search")
                .excludePathPatterns("/reply/getReplies")
                // 上传的图片（头像/帖子配图）是公开静态内容，匿名访客浏览帖子时也需要加载
                .excludePathPatterns("/uploads/**")
                .excludePathPatterns("/swagger*/**")
                .excludePathPatterns("/v3*/**")
                .excludePathPatterns("/dist/**")
                .excludePathPatterns("/image/**")
                .excludePathPatterns("/js/**")
                .excludePathPatterns("/**.ico");
    }
}
