package com.gxy.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.gxy.common.ApiResponse;
import com.gxy.common.exception.BusinessException;
import com.gxy.model.dto.UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    @PostMapping("/images")
    @SaCheckLogin
    public ApiResponse<UploadResponse> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 jpg、jpeg、png、webp 图片");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("图片格式不受支持");
        }

        String dateSegment = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path uploadDir = Paths.get(uploadBaseDir, "images", dateSegment).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path target = uploadDir.resolve(fileName).normalize();
            if (!target.startsWith(uploadDir)) {
                throw new BusinessException("上传路径非法");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String relativePath = "/uploads/images/" + dateSegment + "/" + fileName;
            String url = ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath(relativePath)
                    .replaceQuery(null)
                    .build()
                    .toUriString();
            return ApiResponse.ok(new UploadResponse(url, originalName, file.getSize()));
        } catch (IOException ex) {
            throw new BusinessException("图片上传失败");
        }
    }

    private String getExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            throw new BusinessException("图片缺少文件后缀");
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
