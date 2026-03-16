package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.single.SingleValueStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;

public interface ApplicationConfigStore {

    @SuppressWarnings("rawtypes")
    <T extends KeyValueStore> T requiredStore(Class<T> clazz);

    @SuppressWarnings("rawtypes")
    <T extends SingleValueStore> T requiredSingleStore(Class<T> clazz);

    default AccessKeyStore getAccessKeyStore() {
        return requiredStore(AccessKeyStore.class);
    }

    default FileDownloadEntryStore getFileDownloadEntryStore() {
        return requiredStore(FileDownloadEntryStore.class);
    }

    default DaemonSecretsStore getSecretsConfigStore() {
        return requiredSingleStore(DaemonSecretsStore.class);
    }

    default ShareFileEntryStore getShareFileEntryStore() {
        return requiredStore(ShareFileEntryStore.class);
    }

    default HandshakeTrustedOutStore getHandshakeTrustedOutStore() {
        return requiredStore(HandshakeTrustedOutStore.class);
    }

    default HandshakeTrustedInStore getHandshakeTrustedInStore() {
        return requiredStore(HandshakeTrustedInStore.class);
    }

    default HandshakeSessionOutStore getHandshakeSessionOutStore() {
        return requiredStore(HandshakeSessionOutStore.class);
    }

    default HandshakeSessionInStore getHandshakeSessionInStore() {
        return requiredStore(HandshakeSessionInStore.class);
    }

    void cacheReset();

    class ApplicationConfigStoreInitialized {

    }
}
