package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.Map;

public class CryptoFileOperations<V>
        extends JsonFileOperations<V>
        implements FileOperations<V> {

    private final CryptoTunnel cryptoTunnel;

    private final ApplicationFingerprintSupplier applicationFingerprintSupplier;

    public CryptoFileOperations(FileHelper fileHelper,
                                ObjectMapper objectMapper,
                                Class<V> classType,
                                CryptoTunnel cryptoTunnel,
                                ApplicationFingerprintSupplier applicationFingerprintSupplier) {
        super(fileHelper, objectMapper, classType);
        this.cryptoTunnel = cryptoTunnel;
        this.applicationFingerprintSupplier = applicationFingerprintSupplier;
    }

    @SneakyThrows
    @Override
    protected Map<String, V> deserialize(InputStream inputStream) {
        try (InputStream decryptInputStream = cryptoTunnel.decrypt(inputStream, cryptoTunnel.secretKey(getFingerprint()))) {
            return objectMapper.readValue(decryptInputStream, typeReference);
        }
    }

    @Override
    protected byte[] serialize(Map<String, V> values) {
        byte[] jsonBytes = super.serialize(values);
        return cryptoTunnel.encryptInline(jsonBytes, cryptoTunnel.secretKey(getFingerprint()));
    }

    @SneakyThrows
    private byte[] getFingerprint() {
        String fingerprint = applicationFingerprintSupplier.get();
        String string = cryptoTunnel.getAlgorithm() +
                this.getClass().getName() +
                classType +
                fingerprint;
        return CommonUtils.getFingerprint(string.getBytes()).getBytes();
    }
}
