package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.files.FileEntry;
import com.evolution.dropfile.store.files.FileEntryStore;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

@Component
public class ApiFacade {

    private final AppConfigStore appConfigStore;

    private final KeysConfigStore keysConfigStore;

    private final AccessKeyStore accessKeyStore;

    private final FileEntryStore fileEntryStore;

    private final TunnelClient tunnelClient;

    @Autowired
    public ApiFacade(AppConfigStore appConfigStore,
                     KeysConfigStore keysConfigStore,
                     AccessKeyStore accessKeyStore,
                     FileEntryStore fileEntryStore,
                     TunnelClient tunnelClient) {
        this.appConfigStore = appConfigStore;
        this.keysConfigStore = keysConfigStore;
        this.accessKeyStore = accessKeyStore;
        this.fileEntryStore = fileEntryStore;
        this.tunnelClient = tunnelClient;
    }


    public void daemonShutdown() {
        Executors.newSingleThreadExecutor()
                .submit(() -> {
                    System.exit(0);
                });
    }

    public DaemonInfoResponseDTO daemonInfo() {
        return new DaemonInfoResponseDTO(
                CommonUtils.getFingerprint(keysConfigStore.getRequired().rsa().publicKey()),
                CommonUtils.encodeBase64(keysConfigStore.getRequired().rsa().publicKey()),
                CommonUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey())
        );
    }

    public List<ApiConnectionsAccessInfoResponseDTO> connectionsAccessLs() {
        return accessKeyStore.getAll()
                .entrySet()
                .stream()
                .map(it -> toAccessKeyInfoResponseDTO(it.getKey(), it.getValue()))
                .toList();
    }

    public ApiConnectionsAccessInfoResponseDTO connectionsAccessGenerate(ApiConnectionsAccessGenerateRequestDTO requestDTO) {
        String id = CommonUtils.random();
        String key = CommonUtils.generateSecretNonce16();

        AccessKey accessKey = accessKeyStore.save(
                id,
                new AccessKey(key, Instant.now())
        );

        return toAccessKeyInfoResponseDTO(id, accessKey);
    }

    public boolean connectionsAccessRm(String id) {
        if (ObjectUtils.isEmpty(id)) {
            return false;
        }
        AccessKey accessKey = accessKeyStore.remove(id);
        return accessKey != null;
    }

    public void rmAllAccessKeys() {
        accessKeyStore.removeAll();
    }

    private ApiConnectionsAccessInfoResponseDTO toAccessKeyInfoResponseDTO(String id, AccessKey accessKey) {
        String idAndSecret = id + "+" + accessKey.key();
        return new ApiConnectionsAccessInfoResponseDTO(
                id,
                CommonUtils.encodeBase64(idAndSecret.getBytes()),
                accessKey.created()
        );
    }

    public ApiShareInfoResponseDTO shareAdd(ApiShareAddRequestDTO requestDTO) {
        String id = CommonUtils.random();
        FileEntry entry = fileEntryStore.save(
                id,
                new FileEntry(
                        requestDTO.alias(),
                        requestDTO.absoluteFilePath()
                )
        );
        return toApiFileInfoResponseDTO(id, entry);
    }

    public List<ApiShareInfoResponseDTO> shareLs() {
        return fileEntryStore.getAll()
                .entrySet()
                .stream()
                .map(it -> toApiFileInfoResponseDTO(it.getKey(), it.getValue()))
                .toList();
    }

    public ApiShareInfoResponseDTO shareRm(String id) {
        if (ObjectUtils.isEmpty(id)) {
            return null;
        }
        FileEntry entry = fileEntryStore.remove(id);
        if (entry == null) {
            return null;
        }
        return toApiFileInfoResponseDTO(id, entry);
    }

    private ApiShareInfoResponseDTO toApiFileInfoResponseDTO(String id, FileEntry fileEntry) {
        return new ApiShareInfoResponseDTO(
                id,
                fileEntry.alias(),
                fileEntry.absolutePath()
        );
    }

    public void shareRmAll() {
        fileEntryStore.removeAll();
    }

    public List<ApiConnectionsShareLsResponseDTO> connectionsShareLs() {
        List<FileEntryTunnelResponse> files = tunnelClient.send(
                TunnelClient.Request.builder()
                        .action("share-ls")
                        .build(),
                new TypeReference<List<FileEntryTunnelResponse>>() {
                }
        );
        if (files == null) {
            return Collections.emptyList();
        }
        return files.stream()
                .map(it -> new ApiConnectionsShareLsResponseDTO(it.id(), it.alias()))
                .toList();
    }

    @SneakyThrows
    public ApiConnectionsShareDownloadResponseDTO connectionsShareDownload(ApiConnectionsShareDownloadRequestDTO requestDTO) {
        DownloadFileTunnelResponse responseDTO = tunnelClient.send(
                TunnelClient.Request.builder()
                        .action("share-download")
                        .body(requestDTO.id())
                        .build(),
                DownloadFileTunnelResponse.class
        );
        if (responseDTO == null) {
            return null;
        }

        String downloadDirectory = appConfigStore.getRequired().daemonAppConfig().downloadDirectory();
        File downloadFile;
        if (ObjectUtils.isEmpty(requestDTO.filename())) {
            downloadFile = new File(new File(downloadDirectory), responseDTO.id() + "-" + responseDTO.alias());
        } else {
            downloadFile = new File(new File(downloadDirectory), requestDTO.filename());
        }

        if (Files.notExists(downloadFile.toPath())) {
            Files.createFile(downloadFile.toPath());
        }

        if (!requestDTO.rewrite() && Files.size(downloadFile.toPath()) != 0) {
            throw new RuntimeException("Unable to rewrite file");
        }

        Files.write(downloadFile.toPath(), responseDTO.payload());
        return new ApiConnectionsShareDownloadResponseDTO(downloadFile.getAbsolutePath());
    }

    public String connectionsShareCat(String id) {
        return tunnelClient.send(
                TunnelClient.Request.builder()
                        .action("share-cat")
                        .body(id)
                        .build(),
                String.class
        );
    }
}
