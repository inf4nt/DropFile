package com.evolution.dropfiledaemon.files;

import com.evolution.dropfile.common.dto.LsFileTunnelResponse;
import com.evolution.dropfile.configuration.files.FileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.handler.ActionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LsFileActionHandler implements ActionHandler<Void, LsFileTunnelResponse> {

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
    public LsFileTunnelResponse handle(Void unused) {
        List<LsFileTunnelResponse.LsFileEntry> entries = fileEntryStore
                .getAll()
                .entrySet()
                .stream()
                .map(it -> new LsFileTunnelResponse.LsFileEntry(
                        it.getKey(), it.getValue().alias()
                ))
                .toList();
        return new LsFileTunnelResponse(entries);
    }
}
