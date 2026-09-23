package com.evolution.dropfiledaemon.controller.server;

import com.evolution.dropfile.store.quickshare.QuickShare;
import com.evolution.dropfile.store.quickshare.QuickShareStore;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.service.ApiQuickShareService;
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
import java.nio.file.Files;
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

    private final ApiQuickShareService quickShareService;

    private final StreamingArchiveService streamingArchiveService;

    @GetMapping("/{id}")
    public WebAsyncTask<Void> download(@PathVariable String id, HttpServletResponse response) throws IOException {
        QuickShare activeQuickShare = quickShareStore.update(id, current -> {
            Instant now = Instant.now();

            if (current.expired()) {
                throw new IllegalStateException("QuickShare link already expired: " + id);
            }

            if (quickShareService.isTtlExpired(now, current)) {
                throw new IllegalStateException("QuickShare link expired by TTL: " + id);
            }

            if (current.singleUse()) {
                return current
                        .withExpired(true)
                        .withUpdated(now);
            }

            return current;
        });

        return streamFile(id, activeQuickShare, response);
    }

    private WebAsyncTask<Void> streamFile(String quickShareId, QuickShare quickShare, HttpServletResponse response) {
        if (quickShare.secure()) {
            if (quickShare.directory()) {
                return getSecureDirectory(quickShareId, quickShare, response);
            }
            return getSecureFile(quickShareId, quickShare, response);
        }

        if (daemonApplicationProperties.daemonQuickShareInsecureCompressEnabled) {
            if (quickShare.directory()) {
                return getCompressedInsecureDirectory(quickShare, response);
            }
            return getCompressedInsecureFile(quickShare, response);
        }

        if (quickShare.directory()) {
            return getInsecureDirectory(quickShare, response);
        }
        return getInsecureFile(quickShare, response);
    }

    private WebAsyncTask<Void> getSecureDirectory(String quickShareId,
                                                  QuickShare quickShare,
                                                  HttpServletResponse response) {
        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            Path resourcePath = Paths.get(quickShare.resourceRealPath());
            String filenameContentDisposition = String.format("%s-%s.zip", "secure", quickShareId);

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

    private WebAsyncTask<Void> getSecureFile(String quickShareId,
                                             QuickShare quickShare,
                                             HttpServletResponse response) {
        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            Path resourcePath = Paths.get(quickShare.resourceRealPath());
            String filenameContentDisposition = String.format("%s-%s.zip", "secure", quickShareId);

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

    private WebAsyncTask<Void> getCompressedInsecureFile(QuickShare quickShare,
                                                         HttpServletResponse response) {
        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            Path resourcePath = Paths.get(quickShare.resourceRealPath());
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

    private WebAsyncTask<Void> getCompressedInsecureDirectory(QuickShare quickShare,
                                                              HttpServletResponse response) {
        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            Path resourcePath = Paths.get(quickShare.resourceRealPath());
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
            streamingArchiveService.insecureCompressedZipDirectory(resourcePath, outputStream);
            outputStream.flush();
            return null;
        });
    }

    private WebAsyncTask<Void> getInsecureFile(QuickShare quickShare,
                                               HttpServletResponse response) {
        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            Path resourcePath = Paths.get(quickShare.resourceRealPath());
            String filenameContentDisposition = resourcePath.getFileName().toString();

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setContentLengthLong(Files.size(resourcePath));
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

    private WebAsyncTask<Void> getInsecureDirectory(QuickShare quickShare,
                                                    HttpServletResponse response) {
        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            Path resourcePath = Paths.get(quickShare.resourceRealPath());
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
