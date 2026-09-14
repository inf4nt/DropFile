package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.common.io.WatchdogInputStream;
import com.evolution.dropfile.common.io.WatchdogOutputStream;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.controller.server.ServerQuickShareRestController;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RequiredArgsConstructor
@Component
public class WatchdogStreamingRequestResponseFilter extends OncePerRequestFilter {

    private final DaemonApplicationProperties applicationProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        request = new WatchdogRequestWrapper(
                request,
                applicationProperties.daemonServerServletInputStreamLimitMax,
                Duration.ofMillis(applicationProperties.daemonServerServletInputStreamTimeoutMillis)
        );

        Duration outputTimeout = Duration.ofMillis(applicationProperties.daemonServerServletOutputStreamTimeoutMillis);
        if (!isQuickshareRequest(request) && outputTimeout.isPositive()) {
            response = new WatchdogResponseWrapper(response, outputTimeout);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isQuickshareRequest(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return StringUtils.hasText(servletPath)
                && servletPath.startsWith(ServerQuickShareRestController.QUICKSHARE_ENDPOINT);
    }

    private static class WatchdogRequestWrapper extends HttpServletRequestWrapper {

        private final int limit;

        private final Duration timeout;

        private volatile ServletInputStream servletInputStream;

        public WatchdogRequestWrapper(HttpServletRequest request, int limit, Duration timeout) {
            super(request);
            this.limit = limit;
            this.timeout = timeout;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream result = servletInputStream;
            if (result == null) {
                synchronized (this) {
                    result = servletInputStream;
                    if (result == null) {
                        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(super.getInputStream(), limit, timeout);
                        result = new ServletInputStream() {
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
                        servletInputStream = result;
                    }
                }
            }
            return result;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(), encoding != null ? encoding : StandardCharsets.UTF_8.name()));
        }
    }

    private static class WatchdogResponseWrapper extends HttpServletResponseWrapper {

        private final Duration duration;

        private volatile ServletOutputStreamWrapper servletOutputStream;

        private volatile PrintWriter writer;

        public WatchdogResponseWrapper(HttpServletResponse response, Duration duration) {
            super(response);
            this.duration = duration;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (writer != null) {
                throw new IllegalStateException("getWriter() has already been called on this response.");
            }
            ServletOutputStreamWrapper result = servletOutputStream;
            if (result == null) {
                synchronized (this) {
                    if (writer != null) {
                        throw new IllegalStateException("getWriter() has already been called on this response.");
                    }
                    result = servletOutputStream;
                    if (result == null) {
                        WatchdogOutputStream watchdogOutputStream = new WatchdogOutputStream(super.getOutputStream(), duration);
                        result = new ServletOutputStreamWrapper(watchdogOutputStream);
                        servletOutputStream = result;
                    }
                }
            }
            return result;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (servletOutputStream != null) {
                throw new IllegalStateException("getOutputStream() has already been called on this response.");
            }
            PrintWriter result = writer;
            if (result == null) {
                synchronized (this) {
                    if (servletOutputStream != null) {
                        throw new IllegalStateException("getOutputStream() has already been called on this response.");
                    }
                    result = writer;
                    if (result == null) {
                        String encoding = getCharacterEncoding();
                        result = new PrintWriter(new OutputStreamWriter(getOutputStream(), encoding != null ? encoding : StandardCharsets.UTF_8.name()));
                        writer = result;
                    }
                }
            }
            return result;
        }
    }

    private static class ServletOutputStreamWrapper extends ServletOutputStream {
        private final WatchdogOutputStream watchdogOutputStream;

        public ServletOutputStreamWrapper(WatchdogOutputStream watchdogOutputStream) {
            this.watchdogOutputStream = watchdogOutputStream;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }

        @Override
        public void write(int b) throws IOException {
            watchdogOutputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            watchdogOutputStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            watchdogOutputStream.flush();
        }

        @Override
        public void close() throws IOException {
            watchdogOutputStream.close();
        }
    }
}
