package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public class CryptoFileOperations<V>
        extends JsonFileOperations<V>
        implements FileOperations<V> {

    private final FileProvider fileProvider;

    private final CryptoTunnel cryptoTunnel;

    public CryptoFileOperations(FileHelper fileHelper,
                                ObjectMapper objectMapper,
                                Class<V> classType,
                                FileProvider fileProvider,
                                CryptoTunnel cryptoTunnel) {
        super(fileHelper, objectMapper, classType);
        this.fileProvider = fileProvider;
        this.cryptoTunnel = cryptoTunnel;
    }

    @SneakyThrows
    @Override
    protected Map<String, V> deserialize(InputStream inputStream) {
        byte[] secret = getSecret();
        SecretKey secretKey = cryptoTunnel.secretKey(secret);
        try (InputStream decryptInputStream = cryptoTunnel.decrypt(inputStream, secretKey)) {
            return objectMapper.readValue(decryptInputStream, typeReference);
        }
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
        String string = cryptoTunnel.getAlgorithm() +
                homePath +
                installTime;
        return CommonUtils.getFingerprint(string.getBytes()).getBytes();
    }
}
