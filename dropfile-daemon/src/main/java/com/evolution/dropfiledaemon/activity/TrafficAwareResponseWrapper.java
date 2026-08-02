package com.evolution.dropfiledaemon.activity;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.io.PrintWriter;

public class TrafficAwareResponseWrapper extends HttpServletResponseWrapper {

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    private final ActivityTracker tracker;

    volatile private PrintWriter printWriter;

    volatile private ServletOutputStream servletOutputStream;

    public TrafficAwareResponseWrapper(HttpServletRequest request,
                                       HttpServletResponse response,
                                       ActivityTracker tracker) {
        super(response);
        this.request = request;
        this.response = response;
        this.tracker = tracker;
    }

    private void tryRecordActivity() {
        if (tracker.shouldRecordActivity(request, response)) {
            tracker.recordActivity();
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (printWriter != null) {
            throw new IllegalStateException("getWriter() already called");
        }

        ServletOutputStream result = servletOutputStream;
        if (result == null) {
            synchronized (this) {
                result = servletOutputStream;
                if (result == null) {
                    ServletOutputStream delegate = super.getOutputStream();
                    result = new ServletOutputStream() {
                        @Override
                        public void write(int b) throws IOException {
                            tryRecordActivity();
                            delegate.write(b);
                        }

                        @Override
                        public void write(byte[] b) throws IOException {
                            tryRecordActivity();
                            delegate.write(b);
                        }

                        @Override
                        public void write(byte[] b, int off, int len) throws IOException {
                            tryRecordActivity();
                            delegate.write(b, off, len);
                        }

                        @Override
                        public void flush() throws IOException {
                            tryRecordActivity();
                            delegate.flush();
                        }

                        @Override
                        public boolean isReady() {
                            return delegate.isReady();
                        }

                        @Override
                        public void setWriteListener(WriteListener writeListener) {
                            delegate.setWriteListener(writeListener);
                        }
                    };
                    servletOutputStream = result;
                }
            }
        }
        return result;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (servletOutputStream != null) {
            throw new IllegalStateException("getOutputStream() already called");
        }
        PrintWriter result = printWriter;
        if (result == null) {
            synchronized (this) {
                result = printWriter;
                if (result == null) {
                    PrintWriter delegate = super.getWriter();
                    result = new PrintWriter(delegate, true) {
                        @Override
                        public void write(int c) {
                            tryRecordActivity();
                            super.write(c);
                        }

                        @Override
                        public void write(char[] buf, int off, int len) {
                            tryRecordActivity();
                            super.write(buf, off, len);
                        }

                        @Override
                        public void write(String s, int off, int len) {
                            tryRecordActivity();
                            super.write(s, off, len);
                        }

                        @Override
                        public void flush() {
                            tryRecordActivity();
                            super.flush();
                        }
                    };
                    printWriter = result;
                }
            }
        }
        return result;
    }
}
