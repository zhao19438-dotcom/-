package com.cainiao.forum.services.impl;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.Constants;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.config.UploadProperties;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.services.IFileStorageService;
import com.cainiao.forum.utils.UUIDUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
public class FileStorageServiceImpl implements IFileStorageService {

    @Resource
    private UploadProperties uploadProperties;

    @Override
    public String saveAvatar(Long userId, MultipartFile file) {
        String extension = validateImage(file, Constants.MAX_AVATAR_SIZE, Constants.AVATAR_EXTENSIONS);
        File dir = ensureDirectory("avatar");
        String filename = "avatar_" + userId + "." + extension;
        save(file, new File(dir, filename));

        File[] oldFiles = dir.listFiles((d, name) ->
                name.startsWith("avatar_" + userId + ".") && !name.equals(filename));
        if (oldFiles != null) {
            for (File oldFile : oldFiles) {
                if (!oldFile.delete()) {
                    log.warn("旧头像清理失败: {}", oldFile.getAbsolutePath());
                }
            }
        }
        return "/uploads/avatar/" + filename;
    }

    @Override
    public String saveArticleImage(Long userId, MultipartFile file) {
        String extension = validateImage(file, Constants.MAX_POST_IMAGE_SIZE, Constants.POST_IMAGE_EXTENSIONS);
        File dir = ensureDirectory("article");
        String filename = "img_" + userId + "_" + UUIDUtil.UUID_32() + "." + extension;
        save(file, new File(dir, filename));
        return "/uploads/article/" + filename;
    }

    private String validateImage(MultipartFile file, long maxSize, Set<String> extensions) {
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(AppResult.failed("请选择要上传的图片"));
        }
        if (file.getSize() > maxSize) {
            throw new ApplicationException(AppResult.failed(
                    "图片大小超过限制，请压缩到 " + (maxSize / 1024 / 1024) + "MB 以内"));
        }
        String originalFilename = file.getOriginalFilename();
        int dot = originalFilename == null ? -1 : originalFilename.lastIndexOf('.');
        String extension = dot < 0 ? "" : originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!extensions.contains(extension)) {
            throw new ApplicationException(AppResult.failed("只支持 jpg/jpeg/png/gif 格式的图片"));
        }
        try {
            if (ImageIO.read(file.getInputStream()) == null) {
                throw new ApplicationException(AppResult.failed("文件内容不是有效图片"));
            }
        } catch (IOException e) {
            throw new ApplicationException(AppResult.failed("图片读取失败"));
        }
        return extension;
    }

    private File ensureDirectory(String child) {
        File dir = new File(uploadProperties.getDir(), child);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }
        return dir;
    }

    private void save(MultipartFile file, File destination) {
        try {
            file.transferTo(destination.getAbsoluteFile());
        } catch (IOException e) {
            log.error("图片保存失败: {}", destination.getAbsolutePath(), e);
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }
    }
}
