package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.store.link.QuickShareEntry;
import com.evolution.dropfile.store.link.QuickShareEntryStore;
import com.evolution.dropfiledaemon.util.SecureZipUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.io.File;
import java.io.OutputStream;
import java.time.Instant;

@RequiredArgsConstructor
@RestController
@RequestMapping(PublicQuickShareRestController.ENDPOINT)
public class PublicQuickShareRestController {

    public static final String ENDPOINT = "public/quick-share";

    private final QuickShareEntryStore quickShareEntryStore;

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

        String zipName = String.format("%s-%s.zip", "secure", CommonUtils.random());

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + zipName);
        response.setStatus(200);

        return new WebAsyncTask<>(86400000L, () -> {
            OutputStream outputStream = response.getOutputStream();
            SecureZipUtils.zip(
                    outputStream,
                    new File(quickShareEntry.absolutePath()),
                    quickShareEntry.alias(),
                    quickShareEntry.secret()
            );
            outputStream.flush();
            return null;
        });
    }
}
