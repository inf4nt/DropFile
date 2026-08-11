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

    private final QuickShareEntryStore quickShareEntryStore;

    private final FileHelper fileHelper;

    private final StreamingArchiveService streamingArchiveService;

    @GetMapping("/{id}")
    public WebAsyncTask<Void> download(@PathVariable String id, HttpServletResponse response) throws IOException {
        Map.Entry<String, QuickShareEntry> quickShareEntryEntry = quickShareEntryStore.getRequired(id);
        QuickShareEntry quickShareEntry = quickShareEntryEntry.getValue();

        if (quickShareEntry.expired()) {
            throw new IllegalStateException("Expired " + id);
        }

        if (quickShareEntry.singleUse()) {
            quickShareEntryStore.update(id, value -> value
                    .withExpired(true)
                    .withUpdated(Instant.now())
            );
        }

        File file = new File(quickShareEntry.resourcePath());

        String rawFileNameOrAlias = ObjectUtils.isEmpty(quickShareEntry.fileAlias())
                ? file.getName()
                : quickShareEntry.fileAlias();

        if (quickShareEntry.secure()) {
            if (quickShareEntry.directory()) {
                return getSecureDirectory(quickShareEntryEntry, response);
            }
            return getSecureFile(quickShareEntryEntry, rawFileNameOrAlias, response);
        }

        if (daemonApplicationProperties.daemonQuickShareInsecureCompressEnabled) {
            if (quickShareEntry.directory()) {
                return getCompressedInsecureDirectory(quickShareEntryEntry, rawFileNameOrAlias, response);
            }
            return getCompressedInsecureFile(quickShareEntryEntry, rawFileNameOrAlias, response);
        }

        if (quickShareEntry.directory()) {
            return getInsecureDirectory(quickShareEntryEntry, rawFileNameOrAlias, response);
        }
        return getInsecureFile(quickShareEntryEntry, rawFileNameOrAlias, response);
    }

    private WebAsyncTask<Void> getSecureDirectory(Map.Entry<String, QuickShareEntry> quickShareEntryEntry,
                                                  HttpServletResponse response) {
        String quickShareEntryId = quickShareEntryEntry.getKey();

        response.setContentType("application/zip");
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename(String.format("%s-%s.zip", "secure", quickShareEntryId), StandardCharsets.UTF_8)
                        .build()
                        .toString()
        );
        response.setStatus(200);

        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            OutputStream outputStream = response.getOutputStream();
            QuickShareEntry quickShareEntry = quickShareEntryEntry.getValue();
            streamingArchiveService.secureZipDirectory(
                    Paths.get(quickShareEntry.resourcePath()),
                    quickShareEntry.secret(),
                    outputStream
            );
            outputStream.flush();
            return null;
        });
    }

    private WebAsyncTask<Void> getSecureFile(Map.Entry<String, QuickShareEntry> quickShareEntryEntry,
                                             String rawFileName,
                                             HttpServletResponse response) {
        String quickShareEntryId = quickShareEntryEntry.getKey();

        response.setContentType("application/zip");
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename(String.format("%s-%s.zip", "secure", quickShareEntryId), StandardCharsets.UTF_8)
                        .build()
                        .toString()
        );
        response.setStatus(200);

        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            OutputStream outputStream = response.getOutputStream();
            QuickShareEntry quickShareEntry = quickShareEntryEntry.getValue();
            streamingArchiveService.secureZipFile(
                    Paths.get(quickShareEntry.resourcePath()),
                    rawFileName,
                    quickShareEntry.secret(),
                    outputStream
            );
            outputStream.flush();
            return null;
        });
    }

    private WebAsyncTask<Void> getCompressedInsecureFile(Map.Entry<String, QuickShareEntry> quickShareEntryEntry,
                                                         String rawFileName,
                                                         HttpServletResponse response) {
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
            QuickShareEntry quickShareEntry = quickShareEntryEntry.getValue();
            streamingArchiveService.insecureCompressedZipFile(Paths.get(quickShareEntry.resourcePath()), outputStream);
            outputStream.flush();
            return null;
        });
    }

    private WebAsyncTask<Void> getCompressedInsecureDirectory(Map.Entry<String, QuickShareEntry> quickShareEntryEntry,
                                                              String rawFileName,
                                                              HttpServletResponse response) {
        response.setHeader("Content-Encoding", "gzip");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename(rawFileName + ".zip", StandardCharsets.UTF_8)
                        .build()
                        .toString()
        );
        response.setStatus(200);

        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            OutputStream outputStream = response.getOutputStream();
            QuickShareEntry quickShareEntry = quickShareEntryEntry.getValue();
            streamingArchiveService.insecureCompressedZipDirectory(Paths.get(quickShareEntry.resourcePath()), outputStream);
            outputStream.flush();
            return null;
        });
    }

    private WebAsyncTask<Void> getInsecureFile(Map.Entry<String, QuickShareEntry> quickShareEntryEntry,
                                               String rawFileName,
                                               HttpServletResponse response) {
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
            QuickShareEntry quickShareEntry = quickShareEntryEntry.getValue();
            streamingArchiveService.insecureZipFile(Paths.get(quickShareEntry.resourcePath()), outputStream);
            outputStream.flush();
            return null;
        });
    }

    private WebAsyncTask<Void> getInsecureDirectory(Map.Entry<String, QuickShareEntry> quickShareEntryEntry,
                                                    String rawFileName,
                                                    HttpServletResponse response) {
        response.setContentType("application/zip");
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename(rawFileName + ".zip", StandardCharsets.UTF_8)
                        .build()
                        .toString()
        );
        response.setStatus(200);

        return new WebAsyncTask<>(daemonApplicationProperties.daemonQuickShareSecureAsyncRequestTimeout, () -> {
            OutputStream outputStream = response.getOutputStream();
            QuickShareEntry quickShareEntry = quickShareEntryEntry.getValue();
            streamingArchiveService.insecureZipDirectory(Paths.get(quickShareEntry.resourcePath()), outputStream);
            outputStream.flush();
            return null;
        });
    }
}
