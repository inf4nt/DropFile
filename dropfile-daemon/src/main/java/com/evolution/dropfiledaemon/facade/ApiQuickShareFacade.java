package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiQuickShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiQuickShareLsResponseDTO;
import com.evolution.dropfile.store.link.QuickShareEntry;
import com.evolution.dropfile.store.link.QuickShareEntryStore;
import com.evolution.dropfiledaemon.controller.PublicQuickShareRestController;
import com.evolution.dropfiledaemon.util.InetAddressUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class ApiQuickShareFacade {

    private final Environment environment;

    private final QuickShareEntryStore quickShareEntryStore;

    public ApiQuickShareLsResponseDTO add(ApiQuickShareAddRequestDTO requestDTO) {
        if (Files.notExists(requestDTO.file().toPath())) {
            throw new RuntimeException(new FileNotFoundException(requestDTO.file().toString()));
        }
        if (Files.isDirectory(requestDTO.file().toPath())) {
            throw new UnsupportedOperationException("Directories are unsupported " + requestDTO.file());
        }

        String id = CommonUtils.random();

        String secret = CommonUtils.generateRawSecretNonce12();
        QuickShareEntry quickShareEntry = quickShareEntryStore.save(
                id,
                () -> new QuickShareEntry(
                        requestDTO.file().toPath().toAbsolutePath().toString(),
                        requestDTO.alias(),
                        secret,
                        requestDTO.singleUse(),
                        false,
                        Instant.now(),
                        Instant.now()
                )
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

    private List<ApiQuickShareLsResponseDTO> map(Collection<? extends Map.Entry<String, QuickShareEntry>> linkShareEntries) {
        return linkShareEntries.stream().map(it -> map(it.getKey(), it.getValue())).toList();
    }

    private ApiQuickShareLsResponseDTO map(String id, QuickShareEntry entry) {
        String relativeDownloadLink = buildRelativeDownloadLink(id);
        String downloadLink = buildDownloadLink(id);

        return new ApiQuickShareLsResponseDTO(
                id,
                entry.alias(),
                Paths.get(entry.absolutePath()).toString(),
                entry.secret(),
                relativeDownloadLink,
                List.of(downloadLink),
                entry.singleUse(),
                entry.expired(),
                entry.updated(),
                entry.created()
        );
    }

    private String buildRelativeDownloadLink(String id) {
        return String.format("%s/%s", PublicQuickShareRestController.ENDPOINT, id);
    }

    private String buildDownloadLink(String id) {
        Integer serverPort = Integer.valueOf(environment.getRequiredProperty("server.port"));
        InetAddressUtils.BestLocalAddress bestLocalAddress = InetAddressUtils.getBestLocalAddress();

        InetAddress inetAddress = bestLocalAddress.inetAddress();
        String hostAddress = inetAddress.getHostAddress();
        URI uri = CommonUtils.toURI(hostAddress, serverPort);
        String relativeDownloadLink = buildRelativeDownloadLink(id);
        return String.format("%s|%s/%s", bestLocalAddress.name(), uri, relativeDownloadLink);
    }
}
