package com.evolution.dropfiledaemon.controller.server;

import com.evolution.dropfiledaemon.handshake.HandshakeFacade;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeSessionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping
public class ServerHandshakeRestController {

    public static final String HANDSHAKE_ENDPOINT = "/s/h";

    public static final String HANDSHAKE_SESSION_ENDPOINT = "/s/h/s";

    private final HandshakeFacade handshakeFacade;

    private final ObjectMapper objectMapper;

    @PostMapping(ServerHandshakeRestController.HANDSHAKE_ENDPOINT)
    public WebAsyncTask<HandshakeResponseDTO> handshake(HttpServletRequest httpServletRequest) {
        return new WebAsyncTask<>(() -> {
            HandshakeRequestDTO requestDTO = deserialize(httpServletRequest, HandshakeRequestDTO.class);
            return handshakeFacade.handshake(requestDTO);
        });
    }


    @PostMapping(ServerHandshakeRestController.HANDSHAKE_SESSION_ENDPOINT)
    public WebAsyncTask<HandshakeSessionDTO.Session> sessionHandshake(HttpServletRequest httpServletRequest) {
        return new WebAsyncTask<>(() -> {
            HandshakeSessionDTO.Session requestDTO = deserialize(httpServletRequest, HandshakeSessionDTO.Session.class);
            return handshakeFacade.handshakeSession(requestDTO);
        });
    }

    /**
     * Body is read inside WebAsyncTask on purpose (not via @RequestBody).
     * <p>
     * Spring applies spring.mvc.async.request-timeout only after this method returns WebAsyncTask.
     * With @RequestBody, the container deserializes the request body before the method runs, so that
     * time is outside the async timeout. A slow client can drip the body for a long time and hold the
     * connection without the async timeout firing.
     * <p>
     * Reading httpServletRequest.getInputStream() inside the callable folds body read + handling into
     * the same async timeout window. Keep an input size limit (filter/watchdog); do not "simplify"
     * back to @RequestBody without restoring a separate request-body read timeout.
     */
    private <T> T deserialize(HttpServletRequest request, Class<T> clazz) throws IOException {
        return objectMapper.readValue(request.getInputStream(), clazz);
    }
}
