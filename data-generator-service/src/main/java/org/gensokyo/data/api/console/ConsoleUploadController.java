/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.api.console.dto.InlineUploadRequest;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.model.vo.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Uploads operator files for template V2 file-based sources (path returned to the wizard).
 *
 * @author Gensokyo
 * @since 2026-06-03
 */
@RestController
@RequestMapping("/api/console/uploads")
public class ConsoleUploadController {

    private static final Path UPLOAD_ROOT =
            Paths.get(System.getProperty("user.dir"), "..", "uploaded-sources").normalize();

    /**
     * @param file uploaded bytes
     * @return absolute filesystem path written for the template source {@code path} field
     */
    @PostMapping("/file")
    public R<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "upload.bin");
        String safeName = sanitizeFileName(original);
        Path dest = prepareDest(safeName);
        file.transferTo(dest);
        return R.ok(Const.R_OK, dest.toAbsolutePath().toString());
    }

    /**
     * @param request pasted content and optional filename
     * @return absolute filesystem path for the template source {@code path} field
     */
    @PostMapping("/inline")
    public R<String> uploadInline(@RequestBody InlineUploadRequest request) throws IOException {
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Content is empty");
        }
        String name = request.filename() != null && !request.filename().isBlank()
                ? sanitizeFileName(request.filename())
                : "inline-" + UUID.randomUUID() + ".txt";
        Path dest = prepareDest(name);
        Files.writeString(dest, request.content(), StandardCharsets.UTF_8);
        return R.ok(Const.R_OK, dest.toAbsolutePath().toString());
    }

    private static Path prepareDest(String safeName) throws IOException {
        Files.createDirectories(UPLOAD_ROOT);
        String unique = UUID.randomUUID() + "-" + safeName;
        return UPLOAD_ROOT.resolve(unique);
    }

    private static String sanitizeFileName(String name) {
        String trimmed = name.trim().replace('\\', '/');
        int slash = trimmed.lastIndexOf('/');
        if (slash >= 0) {
            trimmed = trimmed.substring(slash + 1);
        }
        String safe = trimmed.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe.isBlank()) {
            return "upload.bin";
        }
        return safe.toLowerCase(Locale.ROOT);
    }
}
