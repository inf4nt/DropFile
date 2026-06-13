package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import lombok.RequiredArgsConstructor;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Path;

@RequiredArgsConstructor
public class CryptoFileOperationsDecorator implements FileOperations {

    private final FileOperations delegate;

    private final CryptoTunnel cryptoTunnel;

    private final ApplicationFingerprintSupplier applicationFingerprintSupplier;

    @Override
    public void removeAll(Path destination) throws IOException {
        delegate.removeAll(destination);
    }

    @Override
    public void write(Path destination, byte[] bytes) throws IOException {
        SecretKey secretKey = cryptoTunnel.secretKey(getFingerprint());
        byte[] encrypted = cryptoTunnel.encryptInline(bytes, secretKey);
        delegate.write(destination, encrypted);
    }

    @Override
    public byte[] read(Path destination) throws IOException {
        byte[] allBytes = delegate.read(destination);
        byte[] fingerprint = getFingerprint();
        SecretKey secretKey = cryptoTunnel.secretKey(fingerprint);
        return cryptoTunnel.decryptInline(allBytes, secretKey);
    }

    private byte[] getFingerprint() {
        String fingerprint = applicationFingerprintSupplier.get();
        String string = cryptoTunnel.getAlgorithm() +
                this.getClass().getName() +
//                classType +
                fingerprint;
        return CommonUtils.getFingerprint(string.getBytes()).getBytes();
    }
}
