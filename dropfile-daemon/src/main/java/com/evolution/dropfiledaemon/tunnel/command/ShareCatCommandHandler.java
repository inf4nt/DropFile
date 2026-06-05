package com.evolution.dropfiledaemon.tunnel.command;

import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
@Component
public class ShareCatCommandHandler implements CommandHandler<String, InputStream> {

    private static final Integer MAX_CAT_FILE_SIZE = 10 * 1024 * 1024;

    private final ShareFileEntryStore shareFileEntryStore;

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
    public InputStream handle(String id) {
        ShareFileEntry shareFileEntry = shareFileEntryStore.getRequired(id)
                .getValue();

        Path filePath = Paths.get(shareFileEntry.absolutePath());
        if (Files.notExists(filePath)) {
            throw new RuntimeException(String.format("File id %s does not exist %s", id, filePath));
        }
        if (Files.size(filePath) > MAX_CAT_FILE_SIZE) {
            throw new RuntimeException(String.format(
                    "File size exceeds maximum. File id %s file %s", id, filePath
            ));
        }

        return new FileInputStream(shareFileEntry.absolutePath());
    }
}
