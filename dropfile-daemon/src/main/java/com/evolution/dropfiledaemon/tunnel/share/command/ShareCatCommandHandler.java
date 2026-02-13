package com.evolution.dropfiledaemon.tunnel.share.command;

import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@RequiredArgsConstructor
@Component
public class ShareCatCommandHandler implements CommandHandler<String, String> {

    private final ApplicationConfigStore applicationConfigStore;

    @Override
    public String getCommandName() {
        return "share-cat";
    }

    @Override
    public Class<String> getPayloadType() {
        return String.class;
    }

    @SneakyThrows
    @Override
    public String handle(String id) {
        ShareFileEntry shareFileEntry = applicationConfigStore.getShareFileEntryStore().get(id)
                .map(it -> it.getValue())
                .orElse(null);
        if (shareFileEntry == null) {
            return null;
        }

        // TODO possible place to get OutOfMemory. Fix it by hardcoded buffer
        byte[] bytes = Files.readAllBytes(Paths.get(shareFileEntry.absolutePath()));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
