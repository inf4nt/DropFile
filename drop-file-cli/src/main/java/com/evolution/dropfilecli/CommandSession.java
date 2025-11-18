package com.evolution.dropfilecli;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
public class CommandSession {

    @Getter
    @Setter
    private String peerIp = "http://localhost:8080";

    @Getter
    @Setter
    private String downloadDirectory = "D:/.dropfile";
}
