package com.evolution.dropfiledaemon.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain security(HttpSecurity http,
                                        ApplicationAuthFilter filter) throws Exception {
        return http
                .csrf(it -> it.disable())
                .sessionManagement(it -> it.disable())
                .httpBasic(it -> it.disable())
                .formLogin(it -> it.disable())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(it -> {
                    it.requestMatchers("/handshake/**").permitAll();
                    it.anyRequest().permitAll();
                })
                .build();
    }
}