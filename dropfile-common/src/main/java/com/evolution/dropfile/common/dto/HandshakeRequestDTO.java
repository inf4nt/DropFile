package com.evolution.dropfile.common.dto;

import java.net.URI;

public record HandshakeRequestDTO(URI addressURI, String publicKey) {

}
