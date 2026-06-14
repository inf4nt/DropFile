package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.framework.file.ApplicationFingerprintSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

// TODO move to PROD configuration
@Component
public class ApplicationFingerprintSupplierInitializationValidationProcedure {

    @Autowired
    public void init(ApplicationFingerprintSupplier supplier) {
        String key = supplier.get();
        if (ObjectUtils.isEmpty(key)) {
            throw new RuntimeException("ApplicationFingerprintSupplier key must not be empty");
        }
    }
}
