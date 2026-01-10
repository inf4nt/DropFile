package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessGenerateRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ApiConnectionsAccessFacade {

    private final AccessKeyStore accessKeyStore;

    @Autowired
    public ApiConnectionsAccessFacade(AccessKeyStore accessKeyStore) {
        this.accessKeyStore = accessKeyStore;
    }

    public ApiConnectionsAccessInfoResponseDTO generate(ApiConnectionsAccessGenerateRequestDTO requestDTO) {
        String id = CommonUtils.random();
        String key = CommonUtils.generateSecretNonce16();

        AccessKey accessKey = accessKeyStore.save(
                id,
                new AccessKey(key, Instant.now())
        );

        return toAccessKeyInfoResponseDTO(id, accessKey);
    }

    public List<ApiConnectionsAccessInfoResponseDTO> ls() {
        return accessKeyStore.getAll()
                .entrySet()
                .stream()
                .map(it -> toAccessKeyInfoResponseDTO(it.getKey(), it.getValue()))
                .toList();
    }

    public boolean rm(String id) {
        AccessKey remove = accessKeyStore.remove(id);
        return remove != null;
    }

    public void rmAll() {
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
}
