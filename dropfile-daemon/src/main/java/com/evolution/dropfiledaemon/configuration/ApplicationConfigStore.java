package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfile.store.download.FileDownloadEntryStore;
import com.evolution.dropfile.store.secret.SecretsConfigStore;
import com.evolution.dropfile.store.share.ShareFileEntryStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;

public interface ApplicationConfigStore {

    AppConfigStore getAppConfigStore();

    AccessKeyStore getAccessKeyStore();

    FileDownloadEntryStore getFileDownloadEntryStore();

    SecretsConfigStore getSecretsConfigStore();

    ShareFileEntryStore getShareFileEntryStore();

    HandshakeStore getHandshakeStore();

    class ApplicationConfigStoreInitialized {

    }
}
