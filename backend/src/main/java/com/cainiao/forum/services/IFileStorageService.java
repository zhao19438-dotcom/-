package com.cainiao.forum.services;

import org.springframework.web.multipart.MultipartFile;

/** 统一管理公开图片的校验、命名和落盘。 */
public interface IFileStorageService {

    String saveAvatar(Long userId, MultipartFile file);

    String saveArticleImage(Long userId, MultipartFile file);
}
