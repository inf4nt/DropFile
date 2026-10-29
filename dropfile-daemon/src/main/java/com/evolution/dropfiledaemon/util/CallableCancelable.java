package com.evolution.dropfiledaemon.util;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class CallableCancelable<V> implements Callable<V> {

    private final AtomicReference<CallableCancelableState> state = new AtomicReference<>(CallableCancelableState.PENDING);

    private final Callable<V> delegate;

    private final CallableCancelableMiddleware callableCancelableMiddleware;

    @Override
    public V call() throws Exception {
        try {
            CallableCancelableState exchange = state.compareAndExchange(CallableCancelableState.PENDING, CallableCancelableState.RUNNING);
            if (exchange == CallableCancelableState.PENDING) {
                V call = delegate.call();
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                return call;
            }
            if (exchange == CallableCancelableState.RUNNING) {
                throw new IllegalStateException("Already running");
            }
            throw new CancellationException();
        } finally {
            if (state.compareAndSet(CallableCancelableState.RUNNING, CallableCancelableState.CANCELED_FINISHED)) {
                callableCancelableMiddleware.doOnFinally();
            }
        }
    }

    public void cancel() {
        if (state.compareAndSet(CallableCancelableState.PENDING, CallableCancelableState.CANCELED_FINISHED)) {
            callableCancelableMiddleware.doOnCancel();
        }
    }

    private enum CallableCancelableState {
        PENDING,
        RUNNING,
        CANCELED_FINISHED
    }

    public interface CallableCancelableMiddleware {

        void doOnCancel();

        void doOnFinally();
    }

    @RequiredArgsConstructor
    public static class SingleActionCountDownLatchCallableCancelableMiddleware implements CallableCancelableMiddleware {

        private final CountDownLatch countDownLatch;

        @Override
        public void doOnCancel() {
            countDownLatch.countDown();
        }

        @Override
        public void doOnFinally() {
            countDownLatch.countDown();
        }
    }
}
