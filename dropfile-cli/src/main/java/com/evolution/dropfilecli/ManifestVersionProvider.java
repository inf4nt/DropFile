package com.evolution.dropfilecli;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
public class ManifestVersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        Package pkg = this.getClass().getPackage();
        String version = pkg.getImplementationVersion();
        return new String[]{version == null ? "unknown" : version};
    }
}
