package com.evolution.dropfile.store.framework.file;

// TODO rename to InstallationLocalSeedProvider
@FunctionalInterface
public interface ApplicationFingerprintSupplier {

    String get();
}
