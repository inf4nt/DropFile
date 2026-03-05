package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;

public interface ApplicationConfigStore {

    AppConfigStore getAppConfigStore();

    AccessKeyStore getAccessKeyStore();

    FileDownloadEntryStore getFileDownloadEntryStore();

    DaemonSecretsStore getSecretsConfigStore();

    ShareFileEntryStore getShareFileEntryStore();

    HandshakeTrustedOutStore getHandshakeTrustedOutStore();

    HandshakeTrustedInStore getHandshakeTrustedInStore();

    HandshakeSessionOutStore getHandshakeSessionOutStore();

    HandshakeSessionInStore getHandshakeSessionInStore();

    void cacheReset();

    class ApplicationConfigStoreInitialized {

    }
}
