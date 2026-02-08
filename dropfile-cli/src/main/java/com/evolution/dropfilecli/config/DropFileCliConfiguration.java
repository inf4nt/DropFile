package com.evolution.dropfilecli.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.http.HttpClient;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Configuration
public class DropFileCliConfiguration {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat());
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(Instant.class, new JsonSerializer<>() {
            private static final DateTimeFormatter FORMATTER =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneOffset.UTC);

            @Override
            public void serialize(Instant value,
                                  JsonGenerator gen,
                                  SerializerProvider serializers)
                    throws IOException {

                gen.writeString(FORMATTER.format(value));
            }
        });
        objectMapper.registerModule(module);
        return objectMapper;
    }
}
