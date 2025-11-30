package com.evolution.dropfile.configuration.app;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DropFileAppConfig {

    private String downloadDirectory;

    private String daemonAddress;
}
