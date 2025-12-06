package com.evolution.dropfile.configuration.keys;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.security.KeyPair;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DropFileKeysConfig {
    private KeyPair keyPair;
}
