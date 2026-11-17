
package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiHandshakeRequestDTO;
import com.evolution.dropfile.store.access.AccessKey;
import com.evolution.dropfile.store.access.AccessKeyStore;
import com.evolution.dropfile.store.secret.DaemonSecret;
import com.evolution.dropfile.store.secret.DaemonSecretStore;
import com.evolution.dropfiledaemon.DropFileDaemonApplication;
import com.evolution.dropfiledaemon.facade.ApiHandshakeFacade;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionInStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeSessionOutStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.handshake.store.api.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.service.AccessKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class TwoDaemonHandshakeIntegrationTest {

    private static final Duration ACCESS_KEY_TTL = Duration.ofMinutes(10);

    @TempDir
    private Path tempDirectory;

    private ConfigurableApplicationContext daemonA;

    private ConfigurableApplicationContext daemonB;

    @BeforeEach
    void before() {
        daemonA = startDaemon("daemon-a");
        daemonB = startDaemon("daemon-b");
    }

    @AfterEach
    void after() {
        if (daemonB != null) {
            daemonB.close();
        }
        if (daemonA != null) {
            daemonA.close();
        }
    }

    @Test
    void twoDaemonInstancesCompleteHandshake() {
        ApiHandshakeFacade clientFacade =
                daemonA.getBean(ApiHandshakeFacade.class);

        AccessKeyService accessKeyService =
                daemonB.getBean(AccessKeyService.class);

        AccessKeyStore accessKeyStore =
                daemonB.getBean(AccessKeyStore.class);

        String daemonBAddress = getBaseAddress(daemonB);

        AccessKeyService.KeyEnvelope accessKey =
                createAccessKey(accessKeyService, accessKeyStore);

        clientFacade.handshake(
                new ApiHandshakeRequestDTO(daemonBAddress, accessKey.key())
        );

        assertHandshakeCompleted(
                daemonA,
                daemonB,
                accessKeyStore,
                accessKey,
                daemonBAddress
        );
    }

    @Test
    void twoDaemonInstancesCompleteHandshakeThroughApiEndpoint() throws Exception {
        AccessKeyService accessKeyService =
                daemonB.getBean(AccessKeyService.class);

        AccessKeyStore accessKeyStore =
                daemonB.getBean(AccessKeyStore.class);

        ObjectMapper objectMapper =
                daemonA.getBean(ObjectMapper.class);

        String daemonAAddress = getBaseAddress(daemonA);
        String daemonBAddress = getBaseAddress(daemonB);

        AccessKeyService.KeyEnvelope accessKey =
                createAccessKey(accessKeyService, accessKeyStore);

        ApiHandshakeRequestDTO requestDTO =
                new ApiHandshakeRequestDTO(daemonBAddress, accessKey.key());

        byte[] requestBody = objectMapper.writeValueAsBytes(requestDTO);

        DaemonSecret daemonSecret = daemonA.getBean(DaemonSecretStore.class)
                .get()
                .orElseThrow();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(daemonAAddress + "/api/handshake"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + daemonSecret.daemonToken())
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode(), is(200));

        assertHandshakeCompleted(
                daemonA,
                daemonB,
                accessKeyStore,
                accessKey,
                daemonBAddress
        );
    }

    private AccessKeyService.KeyEnvelope createAccessKey(
            AccessKeyService accessKeyService,
            AccessKeyStore accessKeyStore
    ) {
        AccessKeyService.KeyEnvelope accessKey = accessKeyService.generate();

        accessKeyStore.save(
                accessKey.id(),
                new AccessKey(
                        accessKey.key(),
                        ACCESS_KEY_TTL,
                        Instant.now()
                )
        );

        return accessKey;
    }

    private void assertHandshakeCompleted(
            ConfigurableApplicationContext daemonA,
            ConfigurableApplicationContext daemonB,
            AccessKeyStore accessKeyStore,
            AccessKeyService.KeyEnvelope accessKey,
            String daemonBAddress
    ) {
        HandshakeTrustedOutStore trustedOutStore =
                daemonA.getBean(HandshakeTrustedOutStore.class);

        HandshakeTrustedInStore trustedInStore =
                daemonB.getBean(HandshakeTrustedInStore.class);

        HandshakeSessionOutStore sessionOutStore =
                daemonA.getBean(HandshakeSessionOutStore.class);

        HandshakeSessionInStore sessionInStore =
                daemonB.getBean(HandshakeSessionInStore.class);

        // Assert: the access key was consumed.
        assertThat(
                accessKeyStore.get(accessKey.id()).isEmpty(),
                is(true)
        );

        // Assert: daemon A has established trust with daemon B.
        HandshakeTrustedOutStore.TrustedOut trustedOut =
                trustedOutStore.getRequiredLastUpdated().getValue();

        assertThat(trustedOut, notNullValue());
        assertThat(trustedOut.addressEnvelopes(), is(not(empty())));

        assertThat(
                trustedOut.addressEnvelopes().getFirst().address(),
                is(URI.create(daemonBAddress))
        );

        String daemonBFingerprint =
                CommonUtils.getFingerprint(trustedOut.handshake().remoteRSA());

        String daemonAFingerprint =
                CommonUtils.getFingerprint(trustedOut.handshake().publicRSA());

        // Assert: daemon B has established trust with daemon A.
        HandshakeTrustedInStore.TrustedIn trustedIn =
                trustedInStore.getRequired(daemonAFingerprint).getValue();

        assertThat(
                Arrays.equals(
                        trustedOut.handshake().remoteRSA(),
                        trustedIn.handshake().publicRSA()
                ),
                is(true)
        );

        assertThat(
                Arrays.equals(
                        trustedOut.handshake().publicRSA(),
                        trustedIn.handshake().remoteRSA()
                ),
                is(true)
        );

        assertThat(
                trustedOut.sessionId(),
                is(trustedIn.sessionId())
        );

        // Assert: both sides derived matching session keys.
        HandshakeSessionOutStore.SessionOut sessionOut =
                sessionOutStore.getRequired(daemonBFingerprint).getValue();

        HandshakeSessionInStore.SessionIn sessionIn =
                sessionInStore.getRequired(daemonAFingerprint).getValue();

        assertThat(
                Arrays.equals(
                        sessionOut.sessionClientKey(),
                        sessionIn.clientKey()
                ),
                is(true)
        );

        assertThat(
                Arrays.equals(
                        sessionOut.sessionServerKey(),
                        sessionIn.serverKey()
                ),
                is(true)
        );

        assertThat(
                sessionOut.sessionId(),
                is(sessionIn.sessionId())
        );

        assertThat(
                sessionOut.sessionId(),
                is(trustedOut.sessionId())
        );

        assertThat(
                sessionIn.sessionId(),
                is(trustedIn.sessionId())
        );
    }

    private ConfigurableApplicationContext startDaemon(String name) {
        Path home = tempDirectory.resolve(name);

        return new SpringApplicationBuilder(DropFileDaemonApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.main.banner-mode=off",
                        "--spring.lifecycle.timeout-per-shutdown-phase=0",
                        "--dropfile.daemon.application-home.directory=" + home,
                        "--dropfile.daemon.security.replay.max-future-drift-timeout=0"
                );
    }

    private String getBaseAddress(ConfigurableApplicationContext context) {
        WebServerApplicationContext webContext =
                (WebServerApplicationContext) context;

        int port = webContext.getWebServer().getPort();

        return "http://127.0.0.1:%d".formatted(port);
    }
}
