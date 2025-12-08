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
                                        ApiAuthFilter apiAuthFilter,
                                        RequireHandshakeAuthFilter requireHandshakeAuthFilter) throws Exception {
        return http
                .csrf(it -> it.disable())
                .sessionManagement(it -> it.disable())
                .httpBasic(it -> it.disable())
                .formLogin(it -> it.disable())
                .addFilterBefore(apiAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(it -> {
                    it.requestMatchers(apiAuthFilter.getPathPattern()).permitAll();
                })
                .addFilterAfter(requireHandshakeAuthFilter, ApiAuthFilter.class)
                .authorizeHttpRequests(it -> {
                    it.requestMatchers(requireHandshakeAuthFilter.getPathPattern()).permitAll();
                })
                .authorizeHttpRequests(it -> {
                    it.anyRequest().permitAll();
                })
                .build();
    }

//    @Bean
//    public SecurityFilterChain security(HttpSecurity http,
//                                        ApplicationAuthFilter filter,
//                                        RequireHandshakeAuthFilter requireHandshakeAuthFilter) throws Exception {
//        return http
//                .csrf(it -> it.disable())
//                .sessionManagement(it -> it.disable())
//                .httpBasic(it -> it.disable())
//                .formLogin(it -> it.disable())
//                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
//                .authorizeHttpRequests(it -> {
//                    it.requestMatchers("/api/**").permitAll();
//                    it.requestMatchers("/handshake/**").permitAll();
//                })
//                .addFilterAfter(requireHandshakeAuthFilter, ApplicationAuthFilter.class)
//                .authorizeHttpRequests(it -> {
//                    it.requestMatchers("/node/**").permitAll();
//                })
//                .authorizeHttpRequests(it -> {
//                    it.anyRequest().permitAll();
//                })
//                .build();
//    }
}