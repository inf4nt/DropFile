package com.evolution.dropfiledaemon.old;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class ConnectionSession {

    @Getter
    @Setter
    private URI connection;
}
