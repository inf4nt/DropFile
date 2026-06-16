package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import lombok.RequiredArgsConstructor;

import javax.crypto.SecretKey;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@RequiredArgsConstructor
public class CryptoFileOperations implements FileOperations {

    private final FileOperations delegate;

    private final CryptoTunnel cryptoTunnel;

    private final InstallationSeedProvider installationSeedProvider;

    @Override
    public void removeAll(Path destination) throws IOException {
        delegate.removeAll(destination);
    }

    @Override
    public void write(Path destination, InputStream inputStream) throws IOException {
        byte[] fingerprint = getFingerprint();
        SecretKey secretKey = cryptoTunnel.secretKey(fingerprint);
        InputStream shieldStream = new FilterInputStream(inputStream) {
            @Override
            public void close() {

            }
        };

        try (InputStream encryptStream = cryptoTunnel.encryptSealStream(shieldStream, secretKey)) {
            delegate.write(destination, encryptStream);
        }
    }

    @Override
    public InputStream read(Path destination) throws NoContentFoundException, IOException {
        boolean success = false;
        InputStream inputStream = null;
        try {
            inputStream = delegate.read(destination);
            byte[] fingerprint = getFingerprint();
            SecretKey secretKey = cryptoTunnel.secretKey(fingerprint);
            InputStream resultStream = cryptoTunnel.decrypt(inputStream, secretKey);
            success = true;
            return resultStream;
        } finally {
            if (!success && inputStream != null) {
                inputStream.close();
            }
        }
    }

    private byte[] getFingerprint() {
        String seed = installationSeedProvider.get();
        String string = cryptoTunnel.getAlgorithm() +
                this.getClass().getName() +
//                classType +
                seed;
        return CommonUtils.getFingerprint(string.getBytes()).getBytes();
    }
}
