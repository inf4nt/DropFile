package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoTunnelV2;
import com.evolution.dropfile.common.function.OutputStreamConsumer;
import com.evolution.dropfile.common.io.CloseShieldOutputStream;
import com.evolution.dropfile.common.io.InputStreamPipeline;
import com.evolution.dropfile.store.seed.InstallationSeedBootstrapStore;
import lombok.RequiredArgsConstructor;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.UUID;

@RequiredArgsConstructor
public class CryptoFileOperations implements FileOperations {

    private static final byte[] CRYPTO_AAD = "crypto-file-operation-v1-aad".getBytes(StandardCharsets.UTF_8);

    private static final String CRYPTO_INFO = "crypto-file-operation-v1-info";

    private final FileOperations delegate;

    private final CryptoTunnelV2 cryptoTunnel;

    private final InstallationSeedBootstrapStore installationSeedBootstrapStore;

    @Override
    public void removeAll(Path destination) throws IOException {
        delegate.removeAll(destination);
    }

    @Override
    public void write(Path destination, OutputStreamConsumer outputStreamConsumer) throws IOException {
        SecretKey secretKey = getSecretKey();

        delegate.write(destination, delegateOutputStream -> {
            CloseShieldOutputStream closeShieldOutputStream = CloseShieldOutputStream.stream(delegateOutputStream);

            try (OutputStream cipherOutputStream = cryptoTunnel.encryptWrapper(closeShieldOutputStream, CRYPTO_AAD, secretKey)) {
                outputStreamConsumer.accept(cipherOutputStream);
            } catch (GeneralSecurityException e) {
                throw new IOException(e.getMessage(), e);
            }
        });
    }

    @Override
    public InputStream read(Path destination) throws NoContentFoundException, IOException {
        return InputStreamPipeline.from(delegate.read(destination))
                .add(in -> {
                    SecretKey secretKey = getSecretKey();
                    return cryptoTunnel.decryptStreaming(in, CRYPTO_AAD, secretKey);
                })
                .get();
    }

    private SecretKey getSecretKey() throws IOException {
        byte[] rawSecret = getFingerprint();
        try {
            return cryptoTunnel.deriveSecretKey(rawSecret, CRYPTO_INFO);
        } catch (GeneralSecurityException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private byte[] getFingerprint() {
        UUID seed = installationSeedBootstrapStore.getRequired();
        String string = cryptoTunnel.getAlgorithm() +
                CryptoFileOperations.class.getName() +
                seed;
        return CommonUtils.getFingerprint(string.getBytes(StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
    }
}
