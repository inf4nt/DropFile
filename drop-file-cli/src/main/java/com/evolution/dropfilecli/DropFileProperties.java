package com.evolution.dropfilecli;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;
import java.net.URI;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DropFileProperties {

    private URI daemonURI;

    public File homeConfigurationDirectory;

    public File homeDownloadDirectory;
}
