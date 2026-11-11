package com.party.build.verify.service;

import com.party.build.common.api.ErrorCode;
import com.party.build.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED = Set.of("pdf", "jpg", "jpeg", "png", "docx");

    private final Path root;

    public FileStorageService(@Value("${app.file.root}") String root) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (Exception e) {
            throw new IllegalStateException("无法创建文件目录: " + this.root, e);
        }
    }

    public StoredFile save(MultipartFile file, Long personId, Long materialId, int versionNo) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "文件为空");
        }
        if (file.getSize() > 20L * 1024 * 1024) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE, "单份材料不超过 20MB");
        }
        String original = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED.contains(ext)) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "仅支持 PDF/JPG/PNG/DOCX");
        }
        String sha256;
        try (InputStream in = file.getInputStream()) {
            sha256 = sha256(in);
        } catch (Exception e) {
            throw new BizException(ErrorCode.FILE_STORAGE_ERROR, "计算文件哈希失败");
        }
        String relative = "person/" + personId + "/material/" + materialId + "/v" + versionNo
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new BizException(ErrorCode.FILE_STORAGE_ERROR, "非法文件路径");
        }
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new BizException(ErrorCode.FILE_STORAGE_ERROR, "保存文件失败: " + e.getMessage());
        }
        return new StoredFile(relative, sha256, file.getSize(),
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(), original, ext);
    }

    public Path resolve(String relativePath) {
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new BizException(ErrorCode.FILE_STORAGE_ERROR, "非法文件路径");
        }
        return target;
    }

    public Path root() {
        return root;
    }

    private String extension(String name) {
        int idx = name.lastIndexOf('.');
        return idx < 0 ? "" : name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String sha256(InputStream in) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            digest.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public record StoredFile(String relativePath, String sha256, long size, String mimeType,
                             String originalName, String extension) {
    }
}