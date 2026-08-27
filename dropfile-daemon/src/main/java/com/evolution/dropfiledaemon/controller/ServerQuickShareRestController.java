package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.store.quickshare.QuickShare;
import com.evolution.dropfile.store.quickshare.QuickShareStore;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.service.StreamingArchiveService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(ServerQuickShareRestController.QUICKSHARE_ENDPOINT)
public class ServerQuickShareRestController {

    public static final String QUICKSHARE_ENDPOINT = "/s/qs";

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final QuickShareStore quickShareStore;

    private final StreamingArchiveService streamingArchiveService;

    @GetMapping("/{id}")
    public WebAsyncTask<Void> download(@PathVariable String id, HttpServletResponse response) throws IOException {
        Map.Entry<String, QuickShare> quickShareEntryEntry = quickShareStore.getRequired(id);
        QuickShare quickShare = quickShareEntryEntry.getValue();

        if (quickShare.expired()) {
            throw new IllegalStateException("Quickshare file already expired %s".formatted(id));
        }

        if (quickShare.singleUse()) {
            quickShareStore.update(id, value -> value
                    .withExpired(true)
                    .withUpdated(Instant.now())
            );
        }

        if (quickShare.secure()) {
            if (quickShare.directory()) {
                return getSecureDirectory(quickShareEntryEntry, response);
            }
            return getSecureFile(quickShareEntryEntry, response);
        }

        if (daemonApplicationProperties.daemonQuickShareInsecureCompressEnabled) {
            if (quickShare.directory()) {
                return getCompressedInsecureDirectory(quickShareEntryEntry, response);
            }
            return getCompressedInsecureFile(quickShareEntryEntry, response);
        }

        if (quickShare.directory()) {
            return getInsecureDirectory(quickShareEntryEntry, response);
        }
        return getInsecureFile(quickShareEntryEntry, response);
    }

    private WebAsyncTask<Void> getSecureDirectory(Map.Entry<String, QuickShare> quickShareEntryEntry,
                                                  HttpServletResponse response) {
        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            String quickShareEntryId = quickShareEntryEntry.getKey();
            QuickShare quickShare = quickShareEntryEntry.getValue();
            Path resourcePath = Paths.get(quickShare.resourcePath());
            String filenameContentDisposition = String.format("%s-%s.zip", "secure", quickShareEntryId);

            response.setContentType("application/zip");
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                            .filename(filenameContentDisposition, StandardCharsets.UTF_8)
                            .build()
                            .toString()
            );
            response.setStatus(200);

            OutputStream outputStream = response.getOutputStream();
            streamingArchiveService.secureZipDirectory(
                    resourcePath,
                    quickShare.secret(),
                    outputStream
            );
            outputStream.flush();
            return null;
        });
    }

    private WebAsyncTask<Void> getSecureFile(Map.Entry<String, QuickShare> quickShareEntryEntry,
                                             HttpServletResponse response) {
        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            QuickShare quickShare = quickShareEntryEntry.getValue();
            Path resourcePath = Paths.get(quickShare.resourcePath());
            String filenameContentDisposition = String.format("%s-%s.zip", "secure", quickShareEntryEntry.getKey());

            response.setContentType("application/zip");
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                            .filename(filenameContentDisposition, StandardCharsets.UTF_8)
                            .build()
                            .toString()
            );
            response.setStatus(200);

            OutputStream outputStream = response.getOutputStream();
            streamingArchiveService.secureZipFile(
                    resourcePath,
                    resourcePath.getFileName().toString(),
                    quickShare.secret(),
                    outputStream
            );
            outputStream.flush();
            return null;
        });
    }

    private WebAsyncTask<Void> getCompressedInsecureFile(Map.Entry<String, QuickShare> quickShareEntryEntry,
                                                         HttpServletResponse response) {
        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            QuickShare quickShare = quickShareEntryEntry.getValue();
            Path resourcePath = Paths.get(quickShare.resourcePath());
            String filenameContentDisposition = resourcePath.getFileName().toString();

            response.setHeader("Content-Encoding", "gzip");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                            .filename(filenameContentDisposition, StandardCharsets.UTF_8)
                            .build()
                            .toString()
            );
            response.setStatus(200);

            OutputStream outputStream = response.getOutputStream();
            streamingArchiveService.insecureCompressedFile(resourcePath, outputStream);
            outputStream.flush();
            return null;
        });
    }

    private WebAsyncTask<Void> getCompressedInsecureDirectory(Map.Entry<String, QuickShare> quickShareEntryEntry,
                                                              HttpServletResponse response) {
        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            QuickShare quickShare = quickShareEntryEntry.getValue();
            Path resourcePath = Paths.get(quickShare.resourcePath());
            String filenameContentDisposition = resourcePath.getFileName().toString() + ".zip";

            response.setHeader("Content-Encoding", "gzip");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                            .filename(filenameContentDisposition, StandardCharsets.UTF_8)
                            .build()
                            .toString()
            );
            response.setStatus(200);

            OutputStream outputStream = response.getOutputStream();
            streamingArchiveService.insecureCompressedZipDirectory(resourcePath, outputStream);
            outputStream.flush();
            return null;
        });
    }

    private WebAsyncTask<Void> getInsecureFile(Map.Entry<String, QuickShare> quickShareEntryEntry,
                                               HttpServletResponse response) {
        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            QuickShare quickShare = quickShareEntryEntry.getValue();
            Path resourcePath = Paths.get(quickShare.resourcePath());
            String filenameContentDisposition = resourcePath.getFileName().toString();

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                            .filename(filenameContentDisposition, StandardCharsets.UTF_8)
                            .build()
                            .toString()
            );
            response.setStatus(200);

            OutputStream outputStream = response.getOutputStream();
            streamingArchiveService.insecureFile(resourcePath, outputStream);
            outputStream.flush();
            return null;
        });
    }

    private WebAsyncTask<Void> getInsecureDirectory(Map.Entry<String, QuickShare> quickShareEntryEntry,
                                                    HttpServletResponse response) {
        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            QuickShare quickShare = quickShareEntryEntry.getValue();
            Path resourcePath = Paths.get(quickShare.resourcePath());
            String filenameContentDisposition = resourcePath.getFileName().toString() + ".zip";

            response.setContentType("application/zip");
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                            .filename(filenameContentDisposition, StandardCharsets.UTF_8)
                            .build()
                            .toString()
            );
            response.setStatus(200);

            OutputStream outputStream = response.getOutputStream();
            streamingArchiveService.insecureZipDirectory(resourcePath, outputStream);
            outputStream.flush();
            return null;
        });
    }
}
