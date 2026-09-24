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
                                        GlobalRateLimitingFilter rateLimitingFilter,
                                        WatchdogRequestBodyLimiterFilter watchdogFilter,
                                        GlobalOncePerRequestFilter globalFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(watchdogFilter, GlobalRateLimitingFilter.class)
                .addFilterAfter(globalFilter, WatchdogRequestBodyLimiterFilter.class)
                .build();
    }

    @Bean
    public FilterRegistrationBean<GlobalRateLimitingFilter> disableGlobalRateLimitingFilterAutoReg(
            GlobalRateLimitingFilter filter) {
        FilterRegistrationBean<GlobalRateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<WatchdogRequestBodyLimiterFilter> disableWatchdogStreamingRequestResponseFilterAutoReg(
            WatchdogRequestBodyLimiterFilter filter) {
        FilterRegistrationBean<WatchdogRequestBodyLimiterFilter> registration = new FilterRegistrationBean<>(filter);
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
