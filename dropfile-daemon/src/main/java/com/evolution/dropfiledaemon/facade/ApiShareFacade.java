package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiShareInfoResponseDTO;
import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class ApiShareFacade {

    private final ShareFileEntryStore shareFileEntryStore;

    @Autowired
    public ApiShareFacade(ShareFileEntryStore shareFileEntryStore) {
        this.shareFileEntryStore = shareFileEntryStore;
    }

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
        ShareFileEntry entry = shareFileEntryStore.save(
                id,
                new ShareFileEntry(
                        alias,
                        absoluteFilePathPath.toFile().getCanonicalPath()
                )
        );
        return toApiFileInfoResponseDTO(id, entry);
    }

    public List<ApiShareInfoResponseDTO> ls() {
        return shareFileEntryStore.getAll()
                .entrySet()
                .stream()
                .map(it -> toApiFileInfoResponseDTO(it.getKey(), it.getValue()))
                .toList();
    }

    public boolean rm(String id) {
        if (ObjectUtils.isEmpty(id)) {
            return false;
        }
        ShareFileEntry entry = shareFileEntryStore.remove(id);
        return entry != null;
    }

    public void rmAll() {
        shareFileEntryStore.removeAll();
    }

    private ApiShareInfoResponseDTO toApiFileInfoResponseDTO(String id, ShareFileEntry shareFileEntry) {
        return new ApiShareInfoResponseDTO(
                id,
                shareFileEntry.alias(),
                shareFileEntry.absolutePath()
        );
    }
}
