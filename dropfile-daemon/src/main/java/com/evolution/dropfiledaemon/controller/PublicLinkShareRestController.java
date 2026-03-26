package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.store.link.LinkShareEntry;
import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.util.SecureZipUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.time.Instant;

@RequiredArgsConstructor
@RestController
@RequestMapping(PublicLinkShareRestController.LINK_ENDPOINT)
public class PublicLinkShareRestController {

    public static final String LINK_ENDPOINT = "public/link";

    private final ApplicationConfigStore applicationConfigStore;

    @GetMapping("/{id}")
    public WebAsyncTask<StreamingResponseBody> download(@PathVariable String id, HttpServletResponse response) {
        LinkShareEntry linkShareEntry = applicationConfigStore.getLinkShareStore()
                .getRequired(id, value -> !value.used())
                .getValue();
        applicationConfigStore.getLinkShareStore()
                .update(id, value -> value
                        .withUsed(true)
                        .withUpdated(Instant.now())
                );

        ShareFileEntry shareFileEntry = applicationConfigStore.getShareFileEntryStore().getRequired(linkShareEntry.fileId()).getValue();
        File file = new File(shareFileEntry.absolutePath());
        String zipName = String.format("%s-%s.zip", "secure", CommonUtils.random());

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + zipName);
        response.setStatus(200);

        StreamingResponseBody stream = outputStream -> SecureZipUtils.zip(
                outputStream,
                file,
                shareFileEntry.alias(),
                linkShareEntry.secret()
        );
        return new WebAsyncTask<>(-1L, () -> stream);
    }
}
