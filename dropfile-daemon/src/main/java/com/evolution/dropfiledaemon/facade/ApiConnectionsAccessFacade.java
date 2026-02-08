package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessGenerateRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfiledaemon.util.AccessKeyUtils;
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
        AccessKeyUtils.AccessKeyEnvelope accessKeyEnvelope = AccessKeyUtils.generate();

        AccessKey accessKey = accessKeyStore.save(
                accessKeyEnvelope.id(),
                new AccessKey(accessKeyEnvelope.key(), Instant.now())
        );

        return toAccessKeyInfoResponseDTO(accessKeyEnvelope.id(), accessKey);
    }

    public List<ApiConnectionsAccessInfoResponseDTO> ls() {
        return accessKeyStore.getAll()
                .entrySet()
                .stream()
                .map(it -> toAccessKeyInfoResponseDTO(it.getKey(), it.getValue()))
                .toList();
    }

    public void rm(String id) {
        String key = accessKeyStore.getRequiredByKeyStartWith(id).getKey();
        accessKeyStore.remove(key);
    }

    public void rmAll() {
        accessKeyStore.removeAll();
    }

    private ApiConnectionsAccessInfoResponseDTO toAccessKeyInfoResponseDTO(String id, AccessKey accessKey) {
        return new ApiConnectionsAccessInfoResponseDTO(
                id,
                CommonUtils.encodeBase64(accessKey.key().getBytes()),
                accessKey.created()
        );
    }
}
