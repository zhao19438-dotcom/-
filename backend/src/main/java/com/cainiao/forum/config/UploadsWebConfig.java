package com.cainiao.forum.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
// 把上传目录映射为 /uploads/** 静态资源，前端通过该路径访问头像等文件。
public class UploadsWebConfig implements WebMvcConfigurer {

    @Resource
    private UploadProperties uploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = uploadProperties.getDir();
        // location 必须以 / 结尾才被识别为目录。
        String location = dir.endsWith("/") ? "file:" + dir : "file:" + dir + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
