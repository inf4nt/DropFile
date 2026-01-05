package com.evolution.dropfiledaemon.files;

import com.evolution.dropfile.common.dto.FileEntryTunnelResponse;
import com.evolution.dropfile.store.files.FileEntryStore;
import com.evolution.dropfiledaemon.tunnel.framework.ActionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LsFileActionHandler implements ActionHandler<Void, List<FileEntryTunnelResponse>> {

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
    public List<FileEntryTunnelResponse> handle(Void unused) {
        return fileEntryStore
                .getAll()
                .entrySet()
                .stream()
                .map(it -> new FileEntryTunnelResponse(it.getKey(), it.getValue().alias()))
                .toList();
    }
}
