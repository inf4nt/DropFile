package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessGenerateRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.util.AccessKeyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ApiConnectionsAccessFacade {

    private final ApplicationConfigStore applicationConfigStore;

    public ApiConnectionsAccessInfoResponseDTO generate(ApiConnectionsAccessGenerateRequestDTO requestDTO) {
        AccessKeyUtils.AccessKeyEnvelope accessKeyEnvelope = AccessKeyUtils.generate();

        AccessKey accessKey = applicationConfigStore.getAccessKeyStore().save(
                accessKeyEnvelope.id(),
                new AccessKey(accessKeyEnvelope.key(), Instant.now())
        );

        return toAccessKeyInfoResponseDTO(accessKeyEnvelope.id(), accessKey);
    }

    public List<ApiConnectionsAccessInfoResponseDTO> ls() {
        return applicationConfigStore.getAccessKeyStore().getAll()
                .entrySet()
                .stream()
                .map(it -> toAccessKeyInfoResponseDTO(it.getKey(), it.getValue()))
                .toList();
    }

    public void rm(String id) {
        String key = applicationConfigStore.getAccessKeyStore().getRequiredByKeyStartWith(id).getKey();
        applicationConfigStore.getAccessKeyStore().remove(key);
    }

    public void rmAll() {
        applicationConfigStore.getAccessKeyStore().removeAll();
    }

    private ApiConnectionsAccessInfoResponseDTO toAccessKeyInfoResponseDTO(String id, AccessKey accessKey) {
        return new ApiConnectionsAccessInfoResponseDTO(
                id,
                CommonUtils.encodeBase64(accessKey.key().getBytes()),
                accessKey.created()
        );
    }
}
