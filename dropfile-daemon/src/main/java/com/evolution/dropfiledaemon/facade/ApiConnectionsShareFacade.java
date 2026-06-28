package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiConnectionsShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ApiConnectionsShareFacade {

    private final ShareFileEntryStore shareFileEntryStore;

    @SneakyThrows
    public ApiConnectionsShareLsResponseDTO add(ApiConnectionsShareAddRequestDTO requestDTO) {
        String alias = Paths.get(requestDTO.alias()).toString();
        Path absoluteFilePathPath = Paths.get(requestDTO.absoluteFilePath());
        if (Files.notExists(absoluteFilePathPath)) {
            throw new FileNotFoundException(absoluteFilePathPath.toString());
        }
        if (Files.isDirectory(absoluteFilePathPath)) {
            throw new UnsupportedOperationException("Directories are unsupported: " + requestDTO.absoluteFilePath());
        }

        String id = CommonUtils.random();
        ShareFileEntry entry = shareFileEntryStore.save(
                id,
                new ShareFileEntry(
                        alias,
                        absoluteFilePathPath.toFile().getCanonicalPath(),
                        absoluteFilePathPath.toFile().length(),
                        Instant.now()
                )
        );
        return map(id, entry);
    }

    public List<ApiConnectionsShareLsResponseDTO> ls() {
        return shareFileEntryStore.getAll()
                .entrySet()
                .stream()
                .map(it -> map(it.getKey(), it.getValue()))
                .toList();
    }

    public void rm(String id) {
        String key = shareFileEntryStore.getRequiredByKeyStartWith(id).getKey();
        shareFileEntryStore.remove(key);
    }

    public void rmAll() {
        shareFileEntryStore.removeAll();
    }

    private ApiConnectionsShareLsResponseDTO map(String id, ShareFileEntry shareFileEntry) {
        return new ApiConnectionsShareLsResponseDTO(
                id,
                shareFileEntry.alias(),
                shareFileEntry.absolutePath(),
                CommonUtils.toDisplaySize(shareFileEntry.size()),
                shareFileEntry.created()
        );
    }
}
