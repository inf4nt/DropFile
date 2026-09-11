package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiBatchOperationResult;
import com.evolution.dropfile.common.dto.ApiConnectionsShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfile.common.io.FileHelper;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.share.ShareFile;
import com.evolution.dropfile.store.share.ShareFileStore;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.util.RetryExecutor;
import com.evolution.dropfiledaemon.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ApiConnectionsShareFacade {

    private final DaemonApplicationProperties applicationProperties;

    private final FileHelper fileHelper;

    private final ShareFileStore shareFileStore;

    public ApiConnectionsShareLsResponseDTO add(ApiConnectionsShareAddRequestDTO requestDTO, Long timeout) throws IOException, NoSuchAlgorithmException {
        Path path = Paths.get(requestDTO.resourcePath()).toAbsolutePath().normalize();

        if (Files.notExists(path)) {
            throw new FileNotFoundException("No file found %s".formatted(path));
        }

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("File is not a regular file: " + requestDTO.resourcePath());
        }

        Path realPath = path.toRealPath();

        String alias = StringUtils.hasText(requestDTO.alias())
                ? Paths.get(requestDTO.alias()).getFileName().toString()
                : realPath.getFileName().toString();

        String key = CommonUtils.random();
        ShareFile shareFile = shareFileStore.save(key,
                () -> {
                    Instant fileLastModified = Files.getLastModifiedTime(realPath).toInstant();
                    return new ShareFile(
                            alias,
                            realPath.toString(),
                            null,
                            Files.size(realPath),
                            false,
                            fileLastModified,
                            Instant.now()
                    );
                }, value -> {
                    long timeoutMillis = getTimeout(timeout);
                    log.info("Calculating sha256 file {} alias {} timeout {}", realPath, alias, t);
                    String sha256 = calculateSha256(realPath, timeoutMillis);
                    log.info("Calculating sha256 file {} alias {} finished {}", realPath, alias, sha256);
                    return value.withHash(sha256).withAccessible(true);
                });

        return map(key, shareFile);
    }

    private String calculateSha256(Path source, long timeout) {
        return RetryExecutor
                .call(() -> fileHelper.sha256(source))
                .attempts(1)
                .callTimeout(Duration.ofMillis(timeout))
                .run();
    }

    public List<ApiConnectionsShareLsResponseDTO> ls() {
        return shareFileStore.getAll()
                .entrySet()
                .stream()
                .map(it -> map(it.getKey(), it.getValue()))
                .toList();
    }

    public ApiBatchOperationResult rm(Collection<CriteriaEnvelope> shareFileIdCriteriaEnvelopes) {
        KeyValueStore.RemoveResult removeResult = shareFileStore.removeByCriteria(shareFileIdCriteriaEnvelopes);
        return ApiBatchOperationResult.of(
                removeResult.removed(),
                removeResult.notFound(),
                removeResult.ambiguous()
        );
    }

    public void rmAll() {
        shareFileStore.removeAll();
    }

    private ApiConnectionsShareLsResponseDTO map(String id, ShareFile shareFile) {
        boolean accessible = Utils.isAccessible(shareFile);

        return new ApiConnectionsShareLsResponseDTO(
                id,
                shareFile.alias(),
                shareFile.resourcePath(),
                shareFile.hash(),
                CommonUtils.toDisplaySize(shareFile.size()),
                accessible,
                shareFile.created()
        );
    }

    public long getTimeout(Long timeout) {
        return timeout != null && timeout > 0 ? timeout : applicationProperties.daemonShareAddHashExecutionTimeoutMillis;
    }
}
