package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiLinkShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiLinkShareLsResponseDTO;
import com.evolution.dropfile.store.link.LinkShareEntry;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.controller.PublicLinkShareRestController;
import com.evolution.dropfiledaemon.util.InetAddressUtils;
import com.evolution.dropfiledaemon.util.KeyEnvelopeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

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

    private final ApplicationConfigStore applicationConfigStore;

    public ApiLinkShareLsResponseDTO add(ApiLinkShareAddRequestDTO requestDTO) {
        String shareFileId = applicationConfigStore.getShareFileEntryStore().getRequiredByKeyStartWith(requestDTO.fileId()).getKey();

        KeyEnvelopeUtils.KeyEnvelope keyEnvelope = KeyEnvelopeUtils.generate();
        LinkShareEntry linkShareEntry = applicationConfigStore.getLinkShareStore().save(keyEnvelope.id(), () -> new LinkShareEntry(
                shareFileId,
                keyEnvelope.key(),
                false,
                Instant.now(),
                Instant.now()
        ));
        return map(keyEnvelope.id(), linkShareEntry);
    }

    public List<ApiLinkShareLsResponseDTO> ls() {
        Set<Map.Entry<String, LinkShareEntry>> entries = applicationConfigStore.getLinkShareStore().getAll().entrySet();
        return map(entries);
    }

    public void removeByKeyStartWith(String id) {
        String key = applicationConfigStore.getLinkShareStore().getRequiredByKeyStartWith(id).getKey();
        applicationConfigStore.getLinkShareStore().remove(key);
    }

    public void removeAll() {
        applicationConfigStore.getLinkShareStore().removeAll();
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
        String hostAddress = InetAddressUtils.getBestLocalAddress().getHostAddress();
        URI uri = CommonUtils.toURI(hostAddress, serverPort);
        String relativeDownloadLink = buildRelativeDownloadLink(id);
        return String.format("%s/%s", uri, relativeDownloadLink);
    }
}
