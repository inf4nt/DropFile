package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.store.quickshare.QuickShareEntry;
import com.evolution.dropfile.store.quickshare.QuickShareEntryStore;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.service.SecureZipService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.model.enums.CompressionLevel;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.io.File;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping(PublicQuickShareRestController.ENDPOINT)
public class PublicQuickShareRestController {

    public static final String ENDPOINT = "p/qs";

    private final QuickShareEntryStore quickShareEntryStore;

    private final FileHelper fileHelper;

    private final SecureZipService secureZipService;

    private final CompressionLevel secureCompressLevel;

    public PublicQuickShareRestController(QuickShareEntryStore quickShareEntryStore,
                                          FileHelper fileHelper,
                                          SecureZipService secureZipService,
                                          DaemonApplicationProperties applicationProperties) {
        this.quickShareEntryStore = quickShareEntryStore;
        this.fileHelper = fileHelper;
        this.secureZipService = secureZipService;
        this.secureCompressLevel = getSecureCompressLevel(applicationProperties.daemonQuickShareSecureCompressLevel);
    }

    @GetMapping("/{id}")
    public WebAsyncTask<Void> download(@PathVariable String id,
                                       @RequestParam(required = false, name = "compressLevel") Integer compressLevelRequestParam,
                                       HttpServletResponse response) {
        QuickShareEntry quickShareEntry = quickShareEntryStore
                .getRequired(id)
                .getValue();
        if (quickShareEntry.expired()) {
            throw new RuntimeException("Expired " + id);
        }

        if (quickShareEntry.singleUse()) {
            quickShareEntryStore
                    .update(id, value -> value
                            .withExpired(true)
                            .withUpdated(Instant.now())
                    );
        }

        File file = new File(quickShareEntry.absolutePath());

        String responseFileName = ObjectUtils.isEmpty(quickShareEntry.alias())
                ? file.getName()
                : quickShareEntry.alias();

        if (quickShareEntry.secure()) {
            CompressionLevel compressLevel = compressLevelRequestParam != null
                    ? getSecureCompressLevel(compressLevelRequestParam)
                    : this.secureCompressLevel;

            String zipName = String.format("%s-%s.zip", "secure", id);

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=" + zipName);
            response.setStatus(200);

            return new WebAsyncTask<>(86400000L, () -> {
                OutputStream outputStream = response.getOutputStream();
                secureZipService.zip(
                        outputStream,
                        file,
                        responseFileName,
                        quickShareEntry.secret(),
                        compressLevel
                );
                outputStream.flush();
                return null;
            });
        } else {
            String contentType = MediaTypeFactory.getMediaType(responseFileName)
                    .or(() -> MediaTypeFactory.getMediaType(file.getName()))
                    .map(MediaType::toString)
                    .orElse("application/octet-stream");

            response.setContentType(contentType);
            response.setContentLengthLong(file.length());
            response.setHeader("Content-Disposition", "attachment; filename=" + responseFileName);
            response.setStatus(200);

            return new WebAsyncTask<>(86400000L, () -> {
                OutputStream outputStream = response.getOutputStream();
                fileHelper.transferTo(file.toPath(), outputStream);
                outputStream.flush();
                return null;
            });
        }
    }

    private CompressionLevel getSecureCompressLevel(Integer compressLevel) {
        if (compressLevel == null || compressLevel < 0) {
            return CompressionLevel.NO_COMPRESSION;
        }
        return Arrays.stream(CompressionLevel.values()).filter(it -> it.getLevel() == compressLevel).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported compress level " + compressLevel));
    }
}
