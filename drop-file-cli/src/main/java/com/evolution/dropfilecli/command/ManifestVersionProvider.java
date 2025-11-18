package com.evolution.dropfilecli.command;

import picocli.CommandLine;

public class ManifestVersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        Package pkg = this.getClass().getPackage();
        String version = pkg.getImplementationVersion();
        return new String[]{version == null ? "unknown" : version};
    }
}
