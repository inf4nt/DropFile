package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiQuickShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiQuickShareLsResponseDTO;
import com.evolution.dropfile.store.quickshare.QuickShareEntry;
import com.evolution.dropfile.store.quickshare.QuickShareEntryStore;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.controller.PublicQuickShareRestController;
import com.evolution.dropfiledaemon.service.InetLocalAddressService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
@Component
public class ApiQuickShareFacade {

    private final Environment environment;

    private final QuickShareEntryStore quickShareEntryStore;

    private final InetLocalAddressService inetLocalAddressService;

    private final DaemonApplicationProperties applicationProperties;

    public ApiQuickShareLsResponseDTO add(ApiQuickShareAddRequestDTO requestDTO) {
        if (Files.notExists(requestDTO.file().toPath())) {
            throw new RuntimeException(new FileNotFoundException(requestDTO.file().toString()));
        }
        if (Files.isDirectory(requestDTO.file().toPath())) {
            throw new UnsupportedOperationException("Directories are unsupported " + requestDTO.file());
        }

        String id = CommonUtils.random();

        QuickShareEntry quickShareEntry = quickShareEntryStore.save(
                id,
                () -> {
                    if (requestDTO.secure()) {
                        String secret = CommonUtils.generateRawSecretNonce12();
                        return new QuickShareEntry(
                                requestDTO.file().toPath().toAbsolutePath().toString(),
                                requestDTO.alias(),
                                secret,
                                requestDTO.singleUse(),
                                true,
                                false,
                                Instant.now(),
                                Instant.now()
                        );
                    }
                    return new QuickShareEntry(
                            requestDTO.file().toPath().toAbsolutePath().toString(),
                            requestDTO.alias(),
                            null,
                            requestDTO.singleUse(),
                            false,
                            false,
                            Instant.now(),
                            Instant.now()
                    );
                }
        );

        return map(id, quickShareEntry);
    }

    public List<ApiQuickShareLsResponseDTO> ls() {
        Set<Map.Entry<String, QuickShareEntry>> entries = quickShareEntryStore.getAll().entrySet();
        return map(entries);
    }

    public void removeByKeyStartWith(String id) {
        String key = quickShareEntryStore.getRequiredByKeyStartWith(id).getKey();
        quickShareEntryStore.remove(key);
    }

    public void removeAll() {
        quickShareEntryStore.removeAll();
    }

    public ApiQuickShareLsResponseDTO ls(String id) {
        String key = quickShareEntryStore.getRequiredByKeyStartWith(id).getKey();
        Map.Entry<String, QuickShareEntry> entry = quickShareEntryStore.getRequired(key);
        return map(entry.getKey(), entry.getValue());
    }

    private List<ApiQuickShareLsResponseDTO> map(Collection<? extends Map.Entry<String, QuickShareEntry>> linkShareEntries) {
        return linkShareEntries.stream().map(it -> map(it.getKey(), it.getValue())).toList();
    }

    private ApiQuickShareLsResponseDTO map(String linkId, QuickShareEntry entry) {
        String relativeDownloadLink = buildRelativeDownloadLink(linkId);

        @Nullable InetLocalAddressService.ConnectionAddress connectionAddress = inetLocalAddressService.getConnectionAddress();

        List<String> externalLinks = buildExternalLinks(linkId);

        return new ApiQuickShareLsResponseDTO(
                linkId,
                entry.alias(),
                Paths.get(entry.absolutePath()).toString(),
                entry.secret(),
                relativeDownloadLink,
                externalLinks,
                connectionAddress != null ? buildLinks(connectionAddress.wireless(), linkId) : List.of(),
                connectionAddress != null ? buildLinks(connectionAddress.ethernet(), linkId) : List.of(),
                entry.secure(),
                entry.singleUse(),
                entry.expired(),
                entry.updated(),
                entry.created()
        );
    }

    private List<String> buildExternalLinks(String linkId) {
        String daemonExternalHost = applicationProperties.daemonExternalHost;
        if (ObjectUtils.isEmpty(daemonExternalHost)) {
            return Collections.emptyList();
        }

        URI daemonExternalHostURI = CommonUtils.toURI(daemonExternalHost);
        String link = buildLink(daemonExternalHostURI, linkId);
        return List.of(link);
    }

    private List<String> buildLinks(@Nullable InetLocalAddressService.BestLocalAddress address, String linkId) {
        if (address == null) {
            return Collections.emptyList();
        }

        String hostAddress = address.inetAddress().getHostAddress();
        Integer serverPort = Integer.valueOf(environment.getRequiredProperty("server.port"));
        URI daemonRootURI = CommonUtils.toURI(hostAddress, serverPort);
        String link = buildLink(daemonRootURI, linkId);
        return List.of(link);
    }

    private String buildLink(URI daemonRootURI, String linkId) {
        String relativeDownloadLink = buildRelativeDownloadLink(linkId);
        if (daemonRootURI.toString().endsWith("/")) {
            return String.format("%s%s", daemonRootURI, relativeDownloadLink);
        }
        return String.format("%s/%s", daemonRootURI, relativeDownloadLink);
    }

    private String buildRelativeDownloadLink(String id) {
        return String.format("%s/%s", PublicQuickShareRestController.ENDPOINT, id);
    }
}
