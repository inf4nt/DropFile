package com.evolution.dropfiledaemon.files;

import com.evolution.dropfile.common.dto.AddFileRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.handler.ActionHandler;
import org.springframework.stereotype.Component;

@Component
public class AddFileActionHandler implements ActionHandler<AddFileRequestDTO, Void> {

    @Override
    public String getAction() {
        return "add-file";
    }

    @Override
    public Class<AddFileRequestDTO> getPayloadType() {
        return AddFileRequestDTO.class;
    }

    @Override
    public Void handle(AddFileRequestDTO requestDTO) {
        FilesStore.STORE.put(requestDTO.id(), requestDTO);
        return null;
    }
}
