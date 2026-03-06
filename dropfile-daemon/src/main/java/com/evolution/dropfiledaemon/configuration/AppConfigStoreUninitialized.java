package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.app.daemon.DaemonAppConfigStore;

interface AppConfigStoreUninitialized {

    DaemonAppConfigStore getUninitializedDaemonAppConfigStore();
}
