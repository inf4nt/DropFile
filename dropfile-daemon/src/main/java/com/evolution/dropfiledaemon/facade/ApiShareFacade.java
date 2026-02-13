package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiShareInfoResponseDTO;
import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ApiShareFacade {

    private final ApplicationConfigStore applicationConfigStore;

    @SneakyThrows
    public ApiShareInfoResponseDTO add(ApiShareAddRequestDTO requestDTO) {
        String alias = Paths.get(requestDTO.alias()).toString();
        Path absoluteFilePathPath = Paths.get(requestDTO.absoluteFilePath());
        if (Files.notExists(absoluteFilePathPath)) {
            throw new FileNotFoundException(absoluteFilePathPath.toString());
        }
        if (Files.isDirectory(absoluteFilePathPath)) {
            throw new UnsupportedOperationException("Directories are unsupported: " + requestDTO.absoluteFilePath());
        }

        String id = CommonUtils.random();
        ShareFileEntry entry = applicationConfigStore.getShareFileEntryStore().save(
                id,
                new ShareFileEntry(
                        alias,
                        absoluteFilePathPath.toFile().getCanonicalPath()
                )
        );
        return toApiFileInfoResponseDTO(id, entry);
    }

    public List<ApiShareInfoResponseDTO> ls() {
        return applicationConfigStore.getShareFileEntryStore().getAll()
                .entrySet()
                .stream()
                .map(it -> toApiFileInfoResponseDTO(it.getKey(), it.getValue()))
                .toList();
    }

    public void rm(String id) {
        String key = applicationConfigStore.getShareFileEntryStore().getRequiredByKeyStartWith(id).getKey();
        applicationConfigStore.getShareFileEntryStore().remove(key);
    }

    public void rmAll() {
        applicationConfigStore.getShareFileEntryStore().removeAll();
    }

    private ApiShareInfoResponseDTO toApiFileInfoResponseDTO(String id, ShareFileEntry shareFileEntry) {
        return new ApiShareInfoResponseDTO(
                id,
                shareFileEntry.alias(),
                shareFileEntry.absolutePath()
        );
    }
}
