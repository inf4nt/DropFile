package com.evolution.dropfiledaemon.service;

import com.evolution.dropfile.common.function.VoidCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConcurrentTaskServiceTest {

    private ConcurrentTaskService underTest;

    @BeforeEach
    public void before() {
        underTest = new ConcurrentTaskService(100);
    }

    @Test
    void executeFailFast_EmptyCollection_DoesNothing() throws ExecutionException {
        underTest.executeFailFastDownloadProcedure(List.of());
    }

    @Test
    void executeFailFast_Success_ExecutesAllTasks() throws ExecutionException {
        int taskCount = 100;
        AtomicInteger executed = new AtomicInteger();

        List<VoidCallable> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            tasks.add(executed::incrementAndGet);
        }

        underTest.executeFailFastDownloadProcedure(tasks);

        assertThat(executed.get(), is(taskCount));
    }

    @Test
    void executeFailFast_StrictThreadCountBound() throws Exception {
        int threadCount = 100;
        int taskCount = 130;

        CountDownLatch firstBatchStarted = new CountDownLatch(threadCount);
        CountDownLatch releaseTasks = new CountDownLatch(1);
        CountDownLatch callerFinished = new CountDownLatch(1);

        AtomicInteger activeTasks = new AtomicInteger();
        AtomicInteger peakActiveTasks = new AtomicInteger();
        AtomicReference<Throwable> thrown = new AtomicReference<>();

        List<VoidCallable> tasks = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            tasks.add(() -> {
                int active = activeTasks.incrementAndGet();
                peakActiveTasks.accumulateAndGet(active, Math::max);
                firstBatchStarted.countDown();

                try {
                    releaseTasks.await();
                } finally {
                    activeTasks.decrementAndGet();
                }
            });
        }

        Thread callerThread = startExecution(tasks, thrown, null, callerFinished);

        try {
            assertThat(
                    "Eight tasks should start concurrently",
                    firstBatchStarted.await(2, TimeUnit.SECONDS),
                    is(true)
            );

            assertThat(peakActiveTasks.get(), is(threadCount));

            releaseTasks.countDown();

            assertThat(
                    "All tasks should finish",
                    callerFinished.await(5, TimeUnit.SECONDS),
                    is(true)
            );

            assertThat(thrown.get(), nullValue());
            assertThat(activeTasks.get(), is(0));
            assertThat(peakActiveTasks.get(), is(lessThanOrEqualTo(threadCount)));
        } finally {
            releaseTasks.countDown();
            callerThread.join(2_000);
        }
    }

    @Test
    void executeFailFast_Failure_InterruptsOtherRunningTasks() throws Exception {
        int threadCount = 100;

        CountDownLatch allTasksStarted = new CountDownLatch(threadCount);
        CountDownLatch interruptedTasks = new CountDownLatch(threadCount - 1);
        CountDownLatch neverReleased = new CountDownLatch(1);

        List<VoidCallable> tasks = new ArrayList<>();

        tasks.add(() -> {
            allTasksStarted.countDown();

            allTasksStarted.await();

            throw new RuntimeException("Boom!");
        });

        for (int i = 1; i < threadCount; i++) {
            tasks.add(() -> {
                allTasksStarted.countDown();

                try {
                    neverReleased.await();
                } catch (InterruptedException e) {
                    interruptedTasks.countDown();
                    throw e;
                }
            });
        }

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> underTest.executeFailFastDownloadProcedure(tasks)
        );

        assertThat(exception.getMessage(), is("Boom!"));

        assertThat(
                "All other running tasks should be interrupted",
                interruptedTasks.await(4, TimeUnit.SECONDS),
                is(true)
        );
    }

    @Test
    void executeFailFast_Failure_WaitsUntilStubbornTaskFinishes() throws Exception {
        CountDownLatch stubbornTaskStarted = new CountDownLatch(1);
        CountDownLatch stubbornTaskInterrupted = new CountDownLatch(1);
        CountDownLatch allowStubbornTaskToFinish = new CountDownLatch(1);
        CountDownLatch stubbornTaskFinished = new CountDownLatch(1);
        CountDownLatch callerFinished = new CountDownLatch(1);

        AtomicReference<Throwable> thrown = new AtomicReference<>();

        VoidCallable stubbornTask = () -> {
            stubbornTaskStarted.countDown();

            try {
                while (allowStubbornTaskToFinish.getCount() > 0) {
                    try {
                        allowStubbornTaskToFinish.await(50, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ignored) {
                        stubbornTaskInterrupted.countDown();
                    }
                }
            } finally {
                stubbornTaskFinished.countDown();
            }
        };

        VoidCallable failingTask = () -> {
            if (!stubbornTaskStarted.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Stubborn task did not start");
            }

            throw new RuntimeException("Boom!");
        };

        Thread callerThread = startExecution(
                List.of(stubbornTask, failingTask),
                thrown,
                null,
                callerFinished
        );

        try {
            assertThat(
                    "Stubborn task should receive cancellation",
                    stubbornTaskInterrupted.await(2, TimeUnit.SECONDS),
                    is(true)
            );

            assertThat(
                    "Caller must continue waiting for the stubborn task",
                    callerFinished.await(100, TimeUnit.MILLISECONDS),
                    is(false)
            );

            assertThat(stubbornTaskFinished.getCount(), is(1L));

            allowStubbornTaskToFinish.countDown();

            assertThat(
                    "Caller should finish after the stubborn task",
                    callerFinished.await(2, TimeUnit.SECONDS),
                    is(true)
            );

            assertThat(thrown.get(), instanceOf(RuntimeException.class));
            assertThat(thrown.get().getMessage(), is("Boom!"));

            assertThat(stubbornTaskFinished.getCount(), is(0L));
        } finally {
            allowStubbornTaskToFinish.countDown();
            callerThread.join(2_000);
        }
    }

    @Test
    void executeFailFast_CallerInterruptedWhileWaiting_RestoresInterruptStatus()
            throws Exception {
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch callerFinished = new CountDownLatch(1);

        AtomicReference<Throwable> thrown = new AtomicReference<>();
        AtomicBoolean interruptRestored = new AtomicBoolean();

        VoidCallable blockingTask = () -> {
            taskStarted.countDown();

            new CountDownLatch(1).await();
        };

        Thread callerThread = startExecution(
                List.of(blockingTask),
                thrown,
                interruptRestored,
                callerFinished
        );

        assertThat(taskStarted.await(2, TimeUnit.SECONDS), is(true));

        callerThread.interrupt();

        assertThat(
                "Caller should finish after the interrupted task exits",
                callerFinished.await(2, TimeUnit.SECONDS),
                is(true)
        );

        assertThat(thrown.get(), instanceOf(ExecutionException.class));
        assertThat(thrown.get().getCause(), instanceOf(InterruptedException.class));
        assertThat(
                "Caller interrupt status should be restored",
                interruptRestored.get(),
                is(true)
        );
    }

    @Test
    void executeFailFast_OutOfMemoryError_PropagatesError() {
        OutOfMemoryError original = new OutOfMemoryError("Simulated OOM");

        OutOfMemoryError thrown = assertThrows(
                OutOfMemoryError.class,
                () -> underTest.executeFailFastDownloadProcedure(List.of(() -> {
                    throw original;
                }))
        );

        assertThat(thrown, sameInstance(original));
    }

    private Thread startExecution(
            Collection<VoidCallable> tasks,
            AtomicReference<Throwable> thrown,
            AtomicBoolean interruptRestored,
            CountDownLatch callerFinished
    ) {
        Thread callerThread = Thread.ofVirtual().start(() -> {
            try {
                underTest.executeFailFastDownloadProcedure(tasks);
            } catch (Throwable throwable) {
                thrown.set(throwable);
                if (interruptRestored != null) {
                    interruptRestored.set(Thread.currentThread().isInterrupted());
                }
            } finally {
                callerFinished.countDown();
            }
        });

        return callerThread;
    }
}
