package com.evolution.dropfiledaemon.tunnel.share.command;

import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class ShareCatCommandHandler implements CommandHandler<String, String> {

    private final ShareFileEntryStore shareFileEntryStore;

    @Autowired
    public ShareCatCommandHandler(ShareFileEntryStore shareFileEntryStore) {
        this.shareFileEntryStore = shareFileEntryStore;
    }

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
        ShareFileEntry shareFileEntry = shareFileEntryStore.get(id)
                .map(it -> it.getValue())
                .orElse(null);
        if (shareFileEntry == null) {
            return null;
        }
        byte[] bytes = Files.readAllBytes(Paths.get(shareFileEntry.absolutePath()));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
