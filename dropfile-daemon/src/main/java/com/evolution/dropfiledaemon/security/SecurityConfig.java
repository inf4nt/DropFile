package com.evolution.dropfiledaemon.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain security(HttpSecurity http,
                                        WatchdogStreamingRequestResponseFilter watchdogFilter,
                                        GlobalOncePerRequestFilter globalFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(watchdogFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(globalFilter, WatchdogStreamingRequestResponseFilter.class)
                .build();
    }

    @Bean
    public FilterRegistrationBean<WatchdogStreamingRequestResponseFilter> disableWatchdogStreamingRequestResponseFilterAutoReg(
            WatchdogStreamingRequestResponseFilter filter) {
        FilterRegistrationBean<WatchdogStreamingRequestResponseFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<GlobalOncePerRequestFilter> disableGlobalFilterAutoReg(
            GlobalOncePerRequestFilter filter) {
        FilterRegistrationBean<GlobalOncePerRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}