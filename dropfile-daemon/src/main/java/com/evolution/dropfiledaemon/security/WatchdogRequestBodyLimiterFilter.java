package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.common.io.WatchdogInputStream;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Component
public class WatchdogRequestBodyLimiterFilter extends OncePerRequestFilter {

    private final DaemonApplicationProperties applicationProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        request = new WatchdogRequestWrapper(
                request,
                applicationProperties.daemonServerServletInputStreamLimitMax
        );

        filterChain.doFilter(request, response);
    }

    private static class WatchdogRequestWrapper extends HttpServletRequestWrapper {

        private final int limit;

        private volatile ServletInputStream servletInputStream;

        public WatchdogRequestWrapper(HttpServletRequest request, int limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream result = servletInputStream;
            if (result == null) {
                synchronized (this) {
                    result = servletInputStream;
                    if (result == null) {
                        result = getServletInputStream();
                        servletInputStream = result;
                    }
                }
            }
            return result;
        }

        private ServletInputStream getServletInputStream() throws IOException {
            WatchdogInputStream watchdogInputStream = new WatchdogInputStream(super.getInputStream(), limit);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    try {
                        return watchdogInputStream.available() == 0;
                    } catch (IOException e) {
                        return true;
                    }
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() throws IOException {
                    return watchdogInputStream.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    return watchdogInputStream.read(b, off, len);
                }

                @Override
                public void close() throws IOException {
                    watchdogInputStream.close();
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(), encoding != null ? encoding : StandardCharsets.UTF_8.name()));
        }
    }
}
