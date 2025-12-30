package com.evolution.dropfile.common.dto;

import java.net.URI;

@Deprecated
public record HandshakeRequestBodyDTO(URI addressURI, String publicKey) {

}
