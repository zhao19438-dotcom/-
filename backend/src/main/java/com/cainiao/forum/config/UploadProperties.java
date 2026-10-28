package com.cainiao.forum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
// 头像等上传文件的保存目录，来自 application.yml 的 campus-forum.upload-dir。
@ConfigurationProperties(prefix = "campus-forum")
public class UploadProperties {

    // 上传文件根目录，默认相对启动目录的 ./uploads。
    private String uploadDir = "./uploads";

    public String getDir() {
        return uploadDir;
    }
}
