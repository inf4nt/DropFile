package com.evolution.dropfiledaemon.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Fail.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RetryExecutorTest {

    @Test
    public void retryIfNotNullValue() {
        AtomicInteger called = new AtomicInteger(0);

        Boolean result = RetryExecutor
                .call(() -> {
                    called.incrementAndGet();
                    if (called.get() == 2) {
                        return true;
                    }
                    return null;
                })
                .attempts(2)
                .delay(Duration.ofSeconds(0))
                .retryIf(it -> it.result() == null)
                .build()
                .run();
        assertThat(called.get(), is(2));
        assertThat(result, is(true));
    }

    @Test
    public void retryIfException() {
        AtomicInteger called = new AtomicInteger(0);

        Object result = RetryExecutor
                .call(() -> {
                    called.incrementAndGet();
                    if (called.get() == 2) {
                        return null;
                    }
                    throw new RuntimeException();
                })
                .attempts(2)
                .delay(Duration.ofSeconds(0))
                .retryIf(it -> it.exception() != null)
                .build()
                .run();
        assertThat(called.get(), is(2));
        assertNull(result);
    }

    @Test
    public void retryIfHasCalled() {
        AtomicInteger called = new AtomicInteger(0);
        AtomicInteger retryIf = new AtomicInteger(0);
        AtomicReference<List<Integer>> attemptsRetryIf = new AtomicReference<>(new ArrayList<>());
        AtomicReference<List<Exception>> exceptionRetryIf = new AtomicReference<>(new ArrayList<>());
        try {
            RetryExecutor
                    .call(() -> {
                        int i = called.incrementAndGet();
                        throw new RuntimeException("test message " + i);
                    })
                    .retryIf(it -> {
                        attemptsRetryIf.updateAndGet(integers -> {
                            integers.add(it.attempt());
                            return integers;
                        });
                        exceptionRetryIf.updateAndGet(exceptions -> {
                            exceptions.add(it.exception());
                            return exceptions;
                        });
                        retryIf.incrementAndGet();
                        return true;
                    })
                    .attempts(3)
                    .delay(Duration.ofMillis(0))
                    .build()
                    .run();
            fail("Exception not thrown");
        } catch (RetryExecutor.RetryExecutorException e) {
            assertThat(
                    e.getExceptions().size(),
                    is(3)
            );
            assertThat(called.get(), is(3));
            assertThat(retryIf.get(), is(3));
            assertThat(attemptsRetryIf.get(), hasItems(1, 2, 3));
            assertThat(exceptionRetryIf.get().size(), is(3));

            assertThat(
                    exceptionRetryIf.get().stream().map(it -> it.getClass()).distinct().toList().size(),
                    is(1)
            );

            assertThat(
                    exceptionRetryIf.get().stream().map(it -> it.getMessage()).toList(),
                    hasItems(
                            "test message 1",
                            "test message 2",
                            "test message 3"
                    )
            );
        }
    }

    @Test
    public void failsIfResultIsNull() {
        AtomicBoolean called = new AtomicBoolean(false);
        try {
            RetryExecutor
                    .call(() -> {
                        called.set(true);
                        return null;
                    })
                    .attempts(1)
                    .delay(Duration.ofMillis(0))
                    .build()
                    .run();
            fail("Exception not thrown");
        } catch (RetryExecutor.RetryExecutorException e) {
            assertThat(
                    e.getExceptions().size(),
                    is(0)
            );
            assertThat(called.get(), is(true));
        }
    }

    @Test
    public void failsIfExceptionIsThrown() {
        AtomicBoolean called = new AtomicBoolean(false);
        try {
            RetryExecutor
                    .call(() -> {
                        called.set(true);
                        throw new RuntimeException();
                    })
                    .attempts(1)
                    .delay(Duration.ofMillis(0))
                    .build()
                    .run();
            fail("Exception not thrown");
        } catch (RetryExecutor.RetryExecutorException e) {
            assertThat(
                    e.getExceptions().size(),
                    is(1)
            );
            assertThat(called.get(), is(true));
        }
    }

    @Test
    public void throwsDuringCall() {
        AtomicBoolean called = new AtomicBoolean(false);
        try {
            RetryExecutor
                    .call(() -> {
                        called.set(true);
                        throw new RuntimeException("test message");
                    })
                    .attempts(1)
                    .delay(Duration.ofMillis(0))
                    .build()
                    .run();
            fail("Exception not thrown");
        } catch (RetryExecutor.RetryExecutorException e) {
            assertThat(
                    e.getExceptions().size(),
                    is(1)
            );
            Exception exception = e.getExceptions().getFirst();
            assertThat(exception.getClass(), is(RuntimeException.class));
            assertThat(exception.getMessage(), is("test message"));
            assertThat(called.get(), is(true));
        }
    }

    @Test
    public void retry4Times() {
        AtomicInteger counter = new AtomicInteger(0);
        Boolean result = RetryExecutor
                .call(() -> {
                    if (counter.get() == 3) {
                        return true;
                    }
                    counter.incrementAndGet();
                    throw new RuntimeException();
                })
                .attempts(4)
                .delay(Duration.ofMillis(0))
                .build()
                .run();
        assertThat(counter.get(), is(3));
        assertThat(result, is(true));
    }

    @Test
    public void retry4TimesReturnNull() {
        AtomicInteger counter = new AtomicInteger(0);
        Boolean result = RetryExecutor
                .call(() -> {
                    if (counter.get() == 3) {
                        return true;
                    }
                    counter.incrementAndGet();
                    return null;
                })
                .attempts(4)
                .delay(Duration.ofMillis(0))
                .build()
                .run();
        assertThat(counter.get(), is(3));
        assertThat(result, is(true));
    }

    @Test
    public void doOnSuccessful() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger attemptReference = new AtomicInteger(0);
        AtomicBoolean resultReference = new AtomicBoolean(false);

        Boolean run = RetryExecutor
                .call(() -> {
                    if (counter.get() == 3) {
                        return true;
                    }
                    counter.incrementAndGet();
                    return null;
                })
                .attempts(4)
                .doOnSuccessful((attempt, result) -> {
                    attemptReference.set(attempt);
                    resultReference.set(result);
                })
                .delay(Duration.ofMillis(0))
                .build()
                .run();

        assertThat(counter.get(), is(3));
        assertThat(attemptReference.get(), is(4));
        assertThat(resultReference.get(), is(true));
        assertThat(run, is(true));
    }

    @Test
    public void delay() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicReference<List<Long>> dates = new AtomicReference<>(new ArrayList<>());

        Integer attempts = 100;
        Integer stopRetry = attempts - 1;
        Duration delay = Duration.ofMillis(10);

        Boolean run = RetryExecutor
                .call(() -> {
                    if (counter.get() == stopRetry) {
                        counter.incrementAndGet();
                        dates.updateAndGet(longs -> {
                            longs.add(System.currentTimeMillis());
                            return longs;
                        });
                        return true;
                    }
                    counter.incrementAndGet();
                    dates.updateAndGet(longs -> {
                        longs.add(System.currentTimeMillis());
                        return longs;
                    });
                    return null;
                })
                .attempts(attempts)
                .delay(delay)
                .build()
                .run();
        assertThat(counter.get(), is(attempts));
        assertThat(run, is(true));

        assertThat(dates.get().size(), is(attempts));

        List<Long> longs = dates.get();
        for (int i = 0; i < longs.size(); i++) {
            if (i == longs.size() - 1) {
                continue;
            }
            Long first = longs.get(i);
            Long second = longs.get(i + 1);
            long diff = second - first;
            assertThat(diff >= delay.toMillis(), is(true));
        }
    }

    @Test
    public void doOnError() {
        AtomicInteger counter = new AtomicInteger(0);

        AtomicReference<List<Integer>> attemptReference = new AtomicReference<>(new ArrayList<>());
        AtomicReference<List<Exception>> exceptionReference = new AtomicReference<>(new ArrayList<>());

        Boolean run = RetryExecutor
                .call(() -> {
                    if (counter.get() == 3) {
                        counter.incrementAndGet();
                        return true;
                    }
                    counter.incrementAndGet();
                    if (counter.get() - 1 == 1) {
                        throw new RuntimeException("test message " + counter.get());
                    }
                    if (counter.get() - 1 == 2) {
                        throw new IOException("test message " + counter.get());
                    }
                    throw new IllegalArgumentException("test message " + counter.get());
                })
                .doOnError((integer, exception) -> {
                    attemptReference.updateAndGet(integers -> {
                        integers.add(integer);
                        return integers;
                    });
                    exceptionReference.updateAndGet(exceptions -> {
                        exceptions.add(exception);
                        return exceptions;
                    });
                })
                .attempts(4)
                .delay(Duration.ofMillis(0))
                .build()
                .run();

        assertThat(counter.get(), is(4));
        assertThat(run, is(true));

        assertThat(attemptReference.get().size(), is(3));
        assertThat(
                attemptReference.get(),
                is(
                        hasItems(
                                1,
                                2,
                                3
                        )
                )
        );
        List<Class> list = (List) exceptionReference.get().stream().map(it -> it.getClass()).toList();
        assertThat(
                list,
                is(
                        hasItems(
                                RuntimeException.class,
                                IOException.class,
                                IllegalArgumentException.class
                        )
                )
        );
        assertThat(
                exceptionReference.get().stream().map(it -> it.getMessage()).toList(),
                is(
                        hasItems(
                                "test message 1",
                                "test message 2",
                                "test message 3"
                        )
                )
        );
    }

    @Test
    public void exceptionIfRetryReturnFalse() {
        AtomicInteger counter = new AtomicInteger(0);

        try {
            RetryExecutor
                    .call(() -> {
                        counter.incrementAndGet();
                        if (counter.get() == 2) {
                            throw new IllegalArgumentException();
                        }
                        return null;
                    })
                    .retryIf(it -> {
                        if (it.exception() instanceof IllegalArgumentException) {
                            return false;
                        }
                        return it.exception() != null || it.result() == null;
                    })
                    .delay(Duration.ofMillis(0))
                    .build()
                    .run();
            fail("Exception not thrown");
        } catch (Exception e) {
            assertThat(e.getClass(), is(IllegalArgumentException.class));
            assertThat(counter.get(), is(2));
        }
    }
}
