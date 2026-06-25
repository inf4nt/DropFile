package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiLinkShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiLinkShareLsResponseDTO;
import com.evolution.dropfile.store.link.LinkShareEntry;
import com.evolution.dropfile.store.link.LinkShareEntryStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.controller.PublicLinkShareRestController;
import com.evolution.dropfiledaemon.util.InetAddressUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class ApiLinkShareFacade {

    private final Environment environment;

    private final ShareFileEntryStore shareFileEntryStore;

    private final LinkShareEntryStore linkShareEntryStore;

    public ApiLinkShareLsResponseDTO add(ApiLinkShareAddRequestDTO requestDTO) {
        String shareFileId = shareFileEntryStore.getRequiredByKeyStartWith(requestDTO.fileId()).getKey();

        String secret = CommonUtils.generateRawSecretNonce12();
        String id = CommonUtils.random();

        LinkShareEntry linkShareEntry = linkShareEntryStore.save(id, () -> new LinkShareEntry(
                shareFileId,
                secret,
                false,
                Instant.now(),
                Instant.now()
        ));
        return map(id, linkShareEntry);
    }

    public List<ApiLinkShareLsResponseDTO> ls() {
        Set<Map.Entry<String, LinkShareEntry>> entries = linkShareEntryStore.getAll().entrySet();
        return map(entries);
    }

    public void removeByKeyStartWith(String id) {
        String key = linkShareEntryStore.getRequiredByKeyStartWith(id).getKey();
        linkShareEntryStore.remove(key);
    }

    public void removeAll() {
        linkShareEntryStore.removeAll();
    }

    private List<ApiLinkShareLsResponseDTO> map(Collection<? extends Map.Entry<String, LinkShareEntry>> linkShareEntries) {
        return linkShareEntries.stream().map(it -> map(it.getKey(), it.getValue())).toList();
    }

    private ApiLinkShareLsResponseDTO map(String id, LinkShareEntry entry) {
        String relativeDownloadLink = buildRelativeDownloadLink(id);
        String downloadLink = buildDownloadLink(id);

        return new ApiLinkShareLsResponseDTO(
                id,
                entry.fileId(),
                entry.secret(),
                relativeDownloadLink,
                List.of(downloadLink),
                entry.used(),
                entry.updated(),
                entry.created()
        );
    }

    private String buildRelativeDownloadLink(String id) {
        return String.format("%s/%s", PublicLinkShareRestController.LINK_ENDPOINT, id);
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
