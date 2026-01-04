package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.configuration.access.AccessKey;
import com.evolution.dropfile.configuration.access.AccessKeyStore;
import com.evolution.dropfile.configuration.app.AppConfigStore;
import com.evolution.dropfile.configuration.files.FileEntry;
import com.evolution.dropfile.configuration.files.FileEntryStore;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
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
        byte[] publicKeyDH = keysConfigStore.getRequired().dh().publicKey();
        return new DaemonInfoResponseDTO(
                CommonUtils.getFingerprint(publicKeyDH),
                CommonUtils.encodeBase64(publicKeyDH)
        );
    }

    public List<AccessKeyInfoResponseDTO> getAccessKeys() {
        return accessKeyStore.getAll()
                .values()
                .stream()
                .map(it -> toAccessKeyInfoResponseDTO(it))
                .toList();
    }

    public AccessKeyInfoResponseDTO generateAccessKeys(AccessKeyGenerateRequestDTO requestDTO) {
        String id = CommonUtils.generateSecret();
        String key = CommonUtils.generateSecret();

        AccessKey accessKey = accessKeyStore.save(
                id,
                new AccessKey(id, key, Instant.now())
        );

        return toAccessKeyInfoResponseDTO(accessKey);
    }

    public AccessKeyInfoResponseDTO revokeAccessKey(String id) {
        AccessKey accessKey = accessKeyStore.remove(id);
        if (accessKey == null) {
            return null;
        }

        return toAccessKeyInfoResponseDTO(accessKey);
    }

    public void revokeAllAccessKeys() {
        accessKeyStore.removeAll();
    }

    private AccessKeyInfoResponseDTO toAccessKeyInfoResponseDTO(AccessKey accessKey) {
        String idAndSecret = accessKey.id() + "+" + accessKey.key();
        return new AccessKeyInfoResponseDTO(
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
        FileEntry entry = fileEntryStore.remove(id);
        Objects.requireNonNull(entry);
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

    public LsFileTunnelResponse connectionsGetFiles() {
        return tunnelClient.send(
                new TunnelClient.Request("ls-file"),
                LsFileTunnelResponse.class
        );
    }

    @SneakyThrows
    public ApiConnectionsDownloadFileDTO connectionsDownloadFile(String id) {
        DownloadFileTunnelResponse responseDTO = tunnelClient.send(
                new TunnelClient.Request(
                        "download-file",
                        new DownloadFileTunnelRequest(id)
                ),
                DownloadFileTunnelResponse.class
        );
        if (responseDTO == null) {
            return null;
        }

        String downloadDirectory = appConfigStore.getRequired().daemonAppConfig().downloadDirectory();
        File downloadFile = new File(new File(downloadDirectory), responseDTO.id() + "-" + responseDTO.alias());
        if (Files.notExists(downloadFile.toPath())) {
            Files.createFile(downloadFile.toPath());
        }
        Files.write(downloadFile.toPath(), responseDTO.payload());
        return new ApiConnectionsDownloadFileDTO(downloadFile.getAbsolutePath());
    }
}
