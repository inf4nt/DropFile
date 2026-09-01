package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiConnectionsShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareRmResponseDTO;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.share.ShareFile;
import com.evolution.dropfile.store.share.ShareFileStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class ApiConnectionsShareFacade {

    private final ShareFileStore shareFileStore;

    @SneakyThrows
    public ApiConnectionsShareLsResponseDTO add(ApiConnectionsShareAddRequestDTO requestDTO) {
        Path absoluteResourcePath = Paths.get(requestDTO.resourcePath()).toAbsolutePath().normalize();
        String alias = Paths.get(requestDTO.alias()).toString();

        if (Files.notExists(absoluteResourcePath)) {
            throw new FileNotFoundException("No file found %s".formatted(absoluteResourcePath.toString()));
        }

        if (!Files.isRegularFile(absoluteResourcePath)) {
            throw new IllegalArgumentException("File is not a regular file: " + requestDTO.resourcePath());
        }

        String id = CommonUtils.random();
        ShareFile entry = shareFileStore.save(
                id,
                new ShareFile(
                        alias,
                        absoluteResourcePath.toFile().getCanonicalPath(),
                        absoluteResourcePath.toFile().length(),
                        Instant.now()
                )
        );
        return map(id, entry);
    }

    public List<ApiConnectionsShareLsResponseDTO> ls() {
        return shareFileStore.getAll()
                .entrySet()
                .stream()
                .map(it -> map(it.getKey(), it.getValue()))
                .toList();
    }

    public ApiConnectionsShareRmResponseDTO rm(Set<String> idCriteria) {
        KeyValueStore.RemoveResult<ShareFile> removeResult = shareFileStore.removeByCriteria(idCriteria);
        return new ApiConnectionsShareRmResponseDTO(
                removeResult.found(),
                removeResult.notFound(),
                removeResult.ambiguous()
        );
    }

    public void rmAll() {
        shareFileStore.removeAll();
    }

    private ApiConnectionsShareLsResponseDTO map(String id, ShareFile shareFile) {
        return new ApiConnectionsShareLsResponseDTO(
                id,
                shareFile.alias(),
                shareFile.resourcePath(),
                CommonUtils.toDisplaySize(shareFile.size()),
                shareFile.created()
        );
    }
}
