package com.evolution.dropfilecli.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.File;
import java.net.URI;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DropFileConfiguration {

    @JsonProperty("daemon_uri")
    private URI daemonURI;

    @JsonProperty("download_directory")
    private File downloadDirectory;
}
