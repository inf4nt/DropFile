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


    public void shutdown() {
        Executors.newSingleThreadExecutor()
                .submit(() -> {
                    System.exit(0);
                });
    }

    public DaemonInfoResponseDTO getDaemonInfo() {
        return new DaemonInfoResponseDTO(
                CommonUtils.getFingerprint(keysConfigStore.getRequired().rsa().publicKey()),
                CommonUtils.encodeBase64(keysConfigStore.getRequired().rsa().publicKey()),
                CommonUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey())
        );
    }

    public List<AccessKeyInfoResponseDTO> getAccessKeys() {
        return accessKeyStore.getAll()
                .entrySet()
                .stream()
                .map(it -> toAccessKeyInfoResponseDTO(it.getKey(), it.getValue()))
                .toList();
    }

    public AccessKeyInfoResponseDTO generateAccessKeys(AccessKeyGenerateRequestDTO requestDTO) {
        String id = CommonUtils.random();
        String key = CommonUtils.generateSecretNonce16();

        AccessKey accessKey = accessKeyStore.save(
                id,
                new AccessKey(key, Instant.now())
        );

        return toAccessKeyInfoResponseDTO(id, accessKey);
    }

    public boolean rmAccessKey(String id) {
        if (ObjectUtils.isEmpty(id)) {
            return false;
        }
        AccessKey accessKey = accessKeyStore.remove(id);
        return accessKey != null;
    }

    public void rmAllAccessKeys() {
        accessKeyStore.removeAll();
    }

    private AccessKeyInfoResponseDTO toAccessKeyInfoResponseDTO(String id, AccessKey accessKey) {
        String idAndSecret = id + "+" + accessKey.key();
        return new AccessKeyInfoResponseDTO(
                id,
                CommonUtils.encodeBase64(idAndSecret.getBytes()),
                accessKey.created()
        );
    }

    public ApiFileInfoResponseDTO addFile(ApiFileAddRequestDTO requestDTO) {
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

    public List<ApiFileInfoResponseDTO> getFiles() {
        return fileEntryStore.getAll()
                .entrySet()
                .stream()
                .map(it -> toApiFileInfoResponseDTO(it.getKey(), it.getValue()))
                .toList();
    }

    public ApiFileInfoResponseDTO deleteFile(String id) {
        if (ObjectUtils.isEmpty(id)) {
            return null;
        }
        FileEntry entry = fileEntryStore.remove(id);
        if (entry == null) {
            return null;
        }
        return toApiFileInfoResponseDTO(id, entry);
    }

    private ApiFileInfoResponseDTO toApiFileInfoResponseDTO(String id, FileEntry fileEntry) {
        return new ApiFileInfoResponseDTO(
                id,
                fileEntry.alias(),
                fileEntry.absolutePath()
        );
    }

    public void deleteAllFiles() {
        fileEntryStore.removeAll();
    }

    public List<FileEntryResponseDTO> connectionsGetFiles() {
        List<FileEntryTunnelResponse> files = tunnelClient.send(
                TunnelClient.Request.builder()
                        .action("ls-file")
                        .build(),
                new TypeReference<List<FileEntryTunnelResponse>>() {
                }
        );
        if (files == null) {
            return Collections.emptyList();
        }
        return files.stream()
                .map(it -> new FileEntryResponseDTO(it.id(), it.alias()))
                .toList();
    }

    @SneakyThrows
    public ApiConnectionsDownloadFileResponseDTO connectionsDownloadFile(ApiConnectionsDownloadFileRequestDTO requestDTO) {
        DownloadFileTunnelResponse responseDTO = tunnelClient.send(
                TunnelClient.Request.builder()
                        .action("download-file")
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
        return new ApiConnectionsDownloadFileResponseDTO(downloadFile.getAbsolutePath());
    }
}
