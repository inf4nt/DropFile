package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiBatchOperationResult;
import com.evolution.dropfile.common.dto.ApiQuickShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiQuickShareLsResponseDTO;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.quickshare.QuickShare;
import com.evolution.dropfile.store.quickshare.QuickShareStore;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.controller.server.ServerQuickShareRestController;
import com.evolution.dropfiledaemon.service.InetLocalAddressService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
@Component
public class ApiQuickShareFacade {

    private final Environment environment;

    private final QuickShareStore quickShareStore;

    private final InetLocalAddressService inetLocalAddressService;

    private final DaemonApplicationProperties applicationProperties;

    @SneakyThrows
    public ApiQuickShareLsResponseDTO add(ApiQuickShareAddRequestDTO requestDTO) {
        Path resourceAbsolutePath = Paths.get(requestDTO.resourcePath()).toAbsolutePath().normalize().toRealPath();

        if (Files.notExists(resourceAbsolutePath)) {
            throw new FileNotFoundException("No file found %s".formatted(resourceAbsolutePath.toString()));
        }

        String id = CommonUtils.random();

        QuickShare quickShare = quickShareStore.save(
                id,
                () -> {
                    boolean directory = Files.isDirectory(resourceAbsolutePath);
                    Instant createInstantTime = Instant.now();

                    if (requestDTO.secure()) {
                        String secret = Objects.requireNonNullElseGet(requestDTO.secret(), () -> CommonUtils.generateRawSecretNonce12());

                        return new QuickShare(
                                resourceAbsolutePath.toString(),
                                secret,
                                directory,
                                requestDTO.singleUse(),
                                true,
                                false,
                                createInstantTime,
                                createInstantTime
                        );
                    }
                    return new QuickShare(
                            resourceAbsolutePath.toString(),
                            null,
                            directory,
                            requestDTO.singleUse(),
                            false,
                            false,
                            createInstantTime,
                            createInstantTime
                    );
                }
        );

        return map(id, quickShare);
    }

    public List<ApiQuickShareLsResponseDTO> ls() {
        Set<Map.Entry<String, QuickShare>> entries = quickShareStore.getAll().entrySet();
        return map(entries);
    }

    public ApiBatchOperationResult removeByCriteria(Collection<CriteriaEnvelope> quickshareIdCriteriaEnvelopes) {
        KeyValueStore.RemoveResult removeResult = quickShareStore.removeByCriteria(quickshareIdCriteriaEnvelopes);
        return ApiBatchOperationResult.of(
                removeResult.removed(),
                removeResult.notFound(),
                removeResult.ambiguous()
        );
    }

    public void removeAll() {
        quickShareStore.removeAll();
    }

    public ApiQuickShareLsResponseDTO show(CriteriaEnvelope quickshareIdCriteriaEnvelope) {
        String key = quickShareStore.getRequiredByCriteria(quickshareIdCriteriaEnvelope).getKey();
        Map.Entry<String, QuickShare> entry = quickShareStore.getRequired(key);
        return map(entry.getKey(), entry.getValue());
    }

    private List<ApiQuickShareLsResponseDTO> map(Collection<? extends Map.Entry<String, QuickShare>> linkShareEntries) {
        return linkShareEntries.stream().map(it -> map(it.getKey(), it.getValue())).toList();
    }

    @SneakyThrows
    private ApiQuickShareLsResponseDTO map(String linkId, QuickShare entry) {
        String relativeDownloadLink = buildRelativeDownloadLink(linkId);

        InetLocalAddressService.ConnectionAddress connectionAddress = inetLocalAddressService.getConnectionAddress();

        String externalLink = buildExternalLinks(linkId);

        return new ApiQuickShareLsResponseDTO(
                linkId,
                entry.resourcePath(),
                CommonUtils.toDisplaySize(CommonUtils.getSize(Paths.get(entry.resourcePath()))),
                entry.secret(),
                relativeDownloadLink,
                externalLink,
                buildLinks(connectionAddress.wireless(), linkId),
                buildLinks(connectionAddress.ethernet(), linkId),
                entry.directory(),
                entry.secure(),
                entry.singleUse(),
                entry.expired(),
                entry.updated(),
                entry.created()
        );
    }

    @Nullable
    private String buildExternalLinks(String linkId) {
        String daemonExternalHost = applicationProperties.daemonExternalHost;
        if (!StringUtils.hasText(daemonExternalHost)) {
            return null;
        }

        URI daemonExternalHostURI = CommonUtils.toURI(daemonExternalHost);
        String link = buildLink(daemonExternalHostURI, linkId);
        return StringUtils.hasText(link) ? link : null;
    }

    private List<String> buildLinks(Collection<InetLocalAddressService.BestLocalAddress> addresses, String linkId) {
        if (ObjectUtils.isEmpty(addresses)) {
            return Collections.emptyList();
        }

        return addresses.stream()
                .map(address -> {
                    String hostAddress = address.inetAddress().getHostAddress();
                    Integer serverPort = Integer.valueOf(environment.getRequiredProperty("server.port"));
                    URI daemonRootURI = CommonUtils.toURI(hostAddress, serverPort);
                    return buildLink(daemonRootURI, linkId);
                })
                .filter(it -> StringUtils.hasText(it))
                .toList();
    }

    private String buildLink(URI daemonRootURI, String linkId) {
        String relativeDownloadLink = buildRelativeDownloadLink(linkId);
        return CommonUtils.joinPaths(daemonRootURI.toString(), relativeDownloadLink);
    }

    private String buildRelativeDownloadLink(String id) {
        return CommonUtils.joinPaths(ServerQuickShareRestController.QUICKSHARE_ENDPOINT, id);
    }
}
