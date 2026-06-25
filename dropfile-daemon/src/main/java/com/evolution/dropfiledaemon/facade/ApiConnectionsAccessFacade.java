package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.ApiConnectionsAccessGenerateRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfiledaemon.util.KeyEnvelopeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ApiConnectionsAccessFacade {

    private final AccessKeyStore accessKeyStore;

    public ApiConnectionsAccessInfoResponseDTO generate(ApiConnectionsAccessGenerateRequestDTO requestDTO) {
        KeyEnvelopeUtils.KeyEnvelope keyEnvelope = KeyEnvelopeUtils.generate();

        AccessKey accessKey = accessKeyStore.save(
                keyEnvelope.id(),
                new AccessKey(keyEnvelope.key(), Instant.now())
        );

        return toAccessKeyInfoResponseDTO(keyEnvelope.id(), accessKey);
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
                accessKey.key(),
                accessKey.created()
        );
    }
}
