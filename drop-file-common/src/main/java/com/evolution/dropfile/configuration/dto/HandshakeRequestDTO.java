package com.evolution.dropfile.configuration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class HandshakeRequestDTO {

    private byte[] publicKey;

    private int port;
}
