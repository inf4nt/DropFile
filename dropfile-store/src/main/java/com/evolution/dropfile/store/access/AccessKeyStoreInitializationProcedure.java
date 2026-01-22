package com.evolution.dropfile.store.access;

import com.evolution.dropfile.store.store.KeyValueStoreInitializationProcedure;

public class AccessKeyStoreInitializationProcedure
        implements KeyValueStoreInitializationProcedure<AccessKeyStore> {

    @Override
    public void init(AccessKeyStore store) {
        store.getAll(); // triggers files creation
    }
}
