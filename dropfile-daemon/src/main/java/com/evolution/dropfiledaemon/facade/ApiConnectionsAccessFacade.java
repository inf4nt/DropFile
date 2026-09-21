package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiBatchOperationResult;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessGenerateRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfiledaemon.service.AccessKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ApiConnectionsAccessFacade {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(10);

    private final AccessKeyService accessKeyService;

    private final AccessKeyStore accessKeyStore;

    public ApiConnectionsAccessInfoResponseDTO generate(ApiConnectionsAccessGenerateRequestDTO requestDTO) {
        AccessKeyService.KeyEnvelope keyEnvelope = accessKeyService.generate();

        AccessKey accessKey = accessKeyStore.save(
                keyEnvelope.id(),
                new AccessKey(keyEnvelope.key(), ACCESS_TOKEN_TTL, Instant.now())
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

    public ApiBatchOperationResult rm(Collection<CriteriaEnvelope> accessIdCriteriaEnvelopes) {
        KeyValueStore.RemoveResult removeResult = accessKeyStore.removeByCriteria(accessIdCriteriaEnvelopes);
        return ApiBatchOperationResult.of(
                removeResult.removed(),
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
                accessKey.ttl().toMillis(),
                accessKey.created().plus(accessKey.ttl()),
                accessKey.created()
        );
    }
}
