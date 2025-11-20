package com.evolution.dropfilecli.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DropFileCliConfiguration {

    @JsonProperty("daemon_address")
    private String daemonAddress;

    @JsonProperty("download_directory")
    private File downloadDirectory;
}
