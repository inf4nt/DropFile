package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfile.store.quickshare.QuickShareEntry;
import com.evolution.dropfile.store.quickshare.QuickShareEntryStore;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.service.StreamingArchiveService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(ServerQuickShareRestController.QUICKSHARE_ENDPOINT)
public class ServerQuickShareRestController {

    public static final String QUICKSHARE_ENDPOINT = "/s/qs";

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final QuickShareEntryStore quickShareEntryStore;

    private final FileHelper fileHelper;

    private final StreamingArchiveService streamingArchiveService;

    @GetMapping("/{id}")
    public WebAsyncTask<Void> download(@PathVariable String id, HttpServletResponse response) throws IOException {
        QuickShareEntry quickShareEntry = quickShareEntryStore
                .getRequired(id)
                .getValue();

        if (quickShareEntry.expired()) {
            throw new IllegalStateException("Expired " + id);
        }

        if (quickShareEntry.singleUse()) {
            quickShareEntryStore
                    .update(id, value -> value
                            .withExpired(true)
                            .withUpdated(Instant.now())
                    );
        }

        File file = new File(quickShareEntry.resourcePath());

        String rawFileName = ObjectUtils.isEmpty(quickShareEntry.alias())
                ? file.getName()
                : quickShareEntry.alias();

        if (quickShareEntry.secure()) {
            String zipName = String.format("%s-%s.zip", "secure", id);

            response.setContentType("application/zip");
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                            .filename(zipName, StandardCharsets.UTF_8)
                            .build()
                            .toString()
            );
            response.setStatus(200);

            return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
                OutputStream outputStream = response.getOutputStream();
                streamingArchiveService.secureZip(
                        outputStream,
                        file.toPath(),
                        rawFileName,
                        quickShareEntry.secret()
                );
                outputStream.flush();
                return null;
            });
        }

        if (daemonApplicationProperties.daemonQuickShareInsecureCompressEnabled) {
            response.setHeader("Content-Encoding", "gzip");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                            .filename(rawFileName, StandardCharsets.UTF_8)
                            .build()
                            .toString()
            );
            response.setStatus(200);
            return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
                OutputStream outputStream = response.getOutputStream();
                streamingArchiveService.gzip(file.toPath(), outputStream);
                outputStream.flush();
                return null;
            });
        }

        String contentType = MediaTypeFactory.getMediaType(rawFileName)
                .or(() -> MediaTypeFactory.getMediaType(file.getName()))
                .map(MediaType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        response.setContentType(contentType);
        response.setContentLengthLong(file.length());
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename(rawFileName, StandardCharsets.UTF_8)
                        .build()
                        .toString()
        );
        response.setStatus(200);

        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            OutputStream outputStream = response.getOutputStream();
            fileHelper.transferTo(file.toPath(), outputStream);
            outputStream.flush();
            return null;
        });
    }

}
