package com.evolution.dropfiledaemon.files;

import com.evolution.dropfile.common.dto.LsFileResponseDTO;
import com.evolution.dropfile.configuration.files.FileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.handler.ActionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LsFileActionHandler implements ActionHandler<Void, LsFileResponseDTO> {

    private final FileEntryStore fileEntryStore;

    @Autowired
    public LsFileActionHandler(FileEntryStore fileEntryStore) {
        this.fileEntryStore = fileEntryStore;
    }

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
        List<LsFileResponseDTO.LsFileEntry> entries = fileEntryStore.getAll()
                .values()
                .stream()
                .map(it -> new LsFileResponseDTO.LsFileEntry(
                        it.id(), it.alias()
                ))
                .toList();
        return new LsFileResponseDTO(entries);
    }
}
