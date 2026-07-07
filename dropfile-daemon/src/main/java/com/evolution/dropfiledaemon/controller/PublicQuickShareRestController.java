package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.store.quickshare.QuickShareEntry;
import com.evolution.dropfile.store.quickshare.QuickShareEntryStore;
import com.evolution.dropfiledaemon.service.SecureZipService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.io.File;
import java.io.OutputStream;
import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(PublicQuickShareRestController.ENDPOINT)
public class PublicQuickShareRestController {

    public static final String ENDPOINT = "p/qs";

    private final QuickShareEntryStore quickShareEntryStore;

    private final FileHelper fileHelper;

    private final SecureZipService secureZipService;

    @GetMapping("/{id}")
    public WebAsyncTask<Void> download(@PathVariable String id, HttpServletResponse response) {
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

        File file = new File(quickShareEntry.resourcePath());

        String responseFileName = ObjectUtils.isEmpty(quickShareEntry.alias())
                ? file.getName()
                : quickShareEntry.alias();

        if (quickShareEntry.secure()) {
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
                        quickShareEntry.secret()
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
}
