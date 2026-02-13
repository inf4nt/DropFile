package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.app.AppConfigStore;

interface AppConfigStoreUninitialized {

    AppConfigStore getUninitializedAppConfigStore();
}
