package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.ApiConnectionsAccessGenerateRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessRmResponseDTO;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfiledaemon.util.KeyEnvelopeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;

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

    public ApiConnectionsAccessRmResponseDTO rm(Set<String> idCriteria) {
        KeyValueStore.RemoveResult<AccessKey> removeResult = accessKeyStore.removeByCriteria(idCriteria);
        return new ApiConnectionsAccessRmResponseDTO(
                removeResult.found(),
                removeResult.notFound(),
                removeResult.ambiguous()
        );
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
