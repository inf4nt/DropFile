package com.evolution.dropfiledaemon.files;

import com.evolution.dropfile.common.dto.LsFileResponseDTO;
import com.evolution.dropfiledaemon.tunnel.framework.handler.ActionHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LsFileActionHandler implements ActionHandler<Void, LsFileResponseDTO> {

    @Override
    public String getAction() {
        return "ls-file";
    }

    @Override
    public Class<Void> getPayloadType() {
        return Void.class;
    }

    @Override
    public LsFileResponseDTO handle(Void unused) {
        List<LsFileResponseDTO.LsFileEntry> entries = FilesStore.STORE.values()
                .stream()
                .map(it -> new LsFileResponseDTO.LsFileEntry(it.id(), it.alias()))
                .toList();
        return new LsFileResponseDTO(entries);
    }
}
