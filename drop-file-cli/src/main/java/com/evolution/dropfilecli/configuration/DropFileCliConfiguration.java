package com.evolution.dropfilecli.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.File;
import java.net.URI;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DropFileCliConfiguration {

    @Setter
    @JsonProperty("daemon_uri")
    private URI daemonURI;

    @JsonProperty("download_directory")
    private File downloadDirectory;
}
