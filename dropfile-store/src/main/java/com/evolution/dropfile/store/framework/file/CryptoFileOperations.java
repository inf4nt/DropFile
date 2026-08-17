package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
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
import java.util.UUID;

@RequiredArgsConstructor
public class CryptoFileOperations implements FileOperations {

    private final FileOperations delegate;

    private final CryptoTunnel cryptoTunnel;

    private final InstallationSeedBootstrapStore installationSeedBootstrapStore;

    @Override
    public void removeAll(Path destination) throws IOException {
        delegate.removeAll(destination);
    }

    @Override
    public void write(Path destination, OutputStreamConsumer outputStreamConsumer) throws IOException {
        byte[] fingerprint = getFingerprint();
        SecretKey secretKey = cryptoTunnel.secretKey(fingerprint);

        delegate.write(destination, delegateOutputStream -> {
            CloseShieldOutputStream closeShieldOutputStream = CloseShieldOutputStream.stream(delegateOutputStream);

            try (OutputStream cipherOutputStream = cryptoTunnel.encryptWrapper(closeShieldOutputStream, secretKey)) {
                outputStreamConsumer.accept(cipherOutputStream);
            }
        });
    }

    @Override
    public InputStream read(Path destination) throws NoContentFoundException, IOException {
        return InputStreamPipeline.from(delegate.read(destination))
                .add(in -> {
                    byte[] fingerprint = getFingerprint();
                    SecretKey secretKey = cryptoTunnel.secretKey(fingerprint);
                    return cryptoTunnel.decrypt(in, secretKey);
                })
                .get();
    }

    private byte[] getFingerprint() {
        UUID seed = installationSeedBootstrapStore.getRequired();
        String string = cryptoTunnel.getAlgorithm() +
                CryptoFileOperations.class.getName() +
                seed;
        return CommonUtils.getFingerprint(string.getBytes(StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
    }
}
