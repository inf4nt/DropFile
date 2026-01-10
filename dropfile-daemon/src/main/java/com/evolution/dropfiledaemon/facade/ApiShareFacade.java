package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.store.share.ShareFileEntry;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Component
public class ApiShareFacade {

    private final ShareFileEntryStore shareFileEntryStore;

    @Autowired
    public ApiShareFacade(ShareFileEntryStore shareFileEntryStore) {
        this.shareFileEntryStore = shareFileEntryStore;
    }

    public ApiShareInfoResponseDTO shareAdd(ApiShareAddRequestDTO requestDTO) {
        String id = CommonUtils.random();
        ShareFileEntry entry = shareFileEntryStore.save(
                id,
                new ShareFileEntry(
                        requestDTO.alias(),
                        requestDTO.absoluteFilePath()
                )
        );
        return toApiFileInfoResponseDTO(id, entry);
    }

    public List<ApiShareInfoResponseDTO> shareLs() {
        return shareFileEntryStore.getAll()
                .entrySet()
                .stream()
                .map(it -> toApiFileInfoResponseDTO(it.getKey(), it.getValue()))
                .toList();
    }

    public boolean shareRm(String id) {
        if (ObjectUtils.isEmpty(id)) {
            return false;
        }
        ShareFileEntry entry = shareFileEntryStore.remove(id);
        if (entry == null) {
            return false;
        }
        return true;
    }

    public void shareRmAll() {
        shareFileEntryStore.removeAll();
    }

    private ApiShareInfoResponseDTO toApiFileInfoResponseDTO(String id, ShareFileEntry shareFileEntry) {
        return new ApiShareInfoResponseDTO(
                id,
                shareFileEntry.alias(),
                shareFileEntry.absolutePath()
        );
    }
}
