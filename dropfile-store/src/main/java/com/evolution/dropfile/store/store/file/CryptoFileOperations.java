package com.evolution.dropfile.store.store.file;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public class CryptoFileOperations<V>
        extends JsonFileOperations<V>
        implements FileOperations<V> {

    private final FileProvider fileProvider;

    private final CryptoTunnel cryptoTunnel;

    public CryptoFileOperations(ObjectMapper objectMapper,
                                Class<V> classType,
                                FileProvider fileProvider,
                                CryptoTunnel cryptoTunnel) {
        super(objectMapper, classType);
        this.fileProvider = fileProvider;
        this.cryptoTunnel = cryptoTunnel;
    }

    @SneakyThrows
    @Override
    protected Map<String, V> deserialize(byte[] bytes) {
        byte[] secret = getSecret();
        SecretKey secretKey = cryptoTunnel.secretKey(secret);
        byte[] decrypted = cryptoTunnel.decryptInline(bytes, secretKey);
        return objectMapper.readValue(decrypted, typeReference);
    }

    @Override
    protected byte[] serialize(Map<String, V> values) {
        byte[] jsonBytes = super.serialize(values);
        byte[] secret = getSecret();
        SecretKey secretKey = cryptoTunnel.secretKey(secret);
        return cryptoTunnel.encryptInline(jsonBytes, secretKey);
    }

    @SneakyThrows
    private byte[] getSecret() {
        Path homePath = fileProvider.getHomePath();
        long installTime = Files.readAttributes(homePath, BasicFileAttributes.class)
                .creationTime().toInstant().toEpochMilli();
        String string = CryptoFileOperations.class.getName() +
                cryptoTunnel.getAlgorithm() +
                homePath +
                installTime;
        return CommonUtils.getFingerprint(string.getBytes()).getBytes();
    }
}
