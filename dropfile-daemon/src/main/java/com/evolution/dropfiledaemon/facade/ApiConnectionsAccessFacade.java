package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiBatchOperationResult;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessGenerateRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.service.AccessKeyService;
import com.evolution.dropfiledaemon.service.InetLocalAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
@Component
public class ApiConnectionsAccessFacade {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(10);

    private final AccessKeyService accessKeyService;

    private final AccessKeyStore accessKeyStore;

    private final InetLocalAddressService inetLocalAddressService;

    private final DaemonApplicationProperties applicationProperties;

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
                .filter(it -> !accessKeyService.isAccessKeyExpired(it.getValue()))
                .map(it -> toAccessKeyInfoResponseDTO(it.getKey(), it.getValue()))
                .toList();
    }

    public ApiBatchOperationResult rm(Set<CriteriaEnvelope> accessIdCriteriaEnvelopes) {
        KeyValueStore.RemoveResult removeResult = accessKeyStore.removeByCriteria(accessIdCriteriaEnvelopes);
        return ApiBatchOperationResult.of(
                removeResult.removed(),
                removeResult.notFound(),
                removeResult.ambiguous()
        );
    }

    public ApiConnectionsAccessInfoResponseDTO show(CriteriaEnvelope accessKeyIdCriteriaEnvelope) {
        Map.Entry<String, AccessKey> accessKeyEntry = accessKeyStore.getRequiredByCriteriaKey(accessKeyIdCriteriaEnvelope);
        if (accessKeyService.isAccessKeyExpired(accessKeyEntry.getValue())) {
            throw new IllegalStateException("Access key %s already expired".formatted(accessKeyEntry.getKey()));
        }
        return toAccessKeyInfoResponseDTO(accessKeyEntry.getKey(), accessKeyEntry.getValue());
    }

    public void rmAll() {
        accessKeyStore.removeAll();
    }

    private ApiConnectionsAccessInfoResponseDTO toAccessKeyInfoResponseDTO(String id, AccessKey accessKey) {
        List<String> connectionsConnectCommands = getConnectionsConnectCommands(accessKey.key());
        return new ApiConnectionsAccessInfoResponseDTO(
                id,
                accessKey.key(),
                accessKey.ttl().toMillis(),
                accessKey.created().plus(accessKey.ttl()),
                connectionsConnectCommands,
                accessKey.created()
        );
    }

    private List<String> getConnectionsConnectCommands(String accessKey) {
        List<String> commands = new ArrayList<>();
        if (StringUtils.hasText(applicationProperties.daemonExternalHost)) {
            String connectionCommand = getConnectionCommand(applicationProperties.daemonExternalHost, accessKey);
            commands.add(connectionCommand);
        }

        InetLocalAddressService.ConnectionAddress connectionAddress;
        try {
            connectionAddress = inetLocalAddressService.getConnectionAddress();
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
        if (!CollectionUtils.isEmpty(connectionAddress.wireless())) {
            List<String> wirelessCommands = buildConnectionsConnectCommand(connectionAddress.wireless(), accessKey);
            commands.addAll(wirelessCommands);
        } else {
            List<String> ethernetCommands = buildConnectionsConnectCommand(connectionAddress.ethernet(), accessKey);
            commands.addAll(ethernetCommands);
        }
        return commands;
    }

    private List<String> buildConnectionsConnectCommand(Collection<InetLocalAddressService.BestLocalAddress> addresses, String accessKey) {
        if (ObjectUtils.isEmpty(addresses)) {
            return Collections.emptyList();
        }

        return addresses.stream()
                .map(address -> {
                    String hostAddress = address.inetAddress().getHostAddress();
                    int serverPort = applicationProperties.serverPort;
                    URI daemonRootURI = CommonUtils.toURI(hostAddress, serverPort);
                    return getConnectionCommand(daemonRootURI.toString(), accessKey);
                })
                .toList();
    }

    private String getConnectionCommand(String daemonAddress, String accessKey) {
        return "dropf connections connect %s --secret %s".formatted(daemonAddress, accessKey);
    }
}
