package com.evolution.dropfiledaemon.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConcurrentTaskServiceTest {

    private ConcurrentTaskService taskService;

    private TrackingExecutorService executorService;

    @BeforeEach
    void setUp() {
        taskService = new ConcurrentTaskService();
        executorService = new TrackingExecutorService(10);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void executeFailFast_StrictThreadCountBound() throws ExecutionException {
        int threadCount = 2;
        int totalTasks = 50;

        AtomicInteger activeTasks = new AtomicInteger();
        AtomicInteger peakActiveTasks = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < totalTasks; i++) {
            tasks.add(() -> {
                int active = activeTasks.incrementAndGet();
                peakActiveTasks.accumulateAndGet(active, Math::max);

                try {
                    Thread.sleep(30);
                } finally {
                    activeTasks.decrementAndGet();
                }

                return null;
            });
        }

        taskService.executeFailFast(tasks, threadCount, executorService);

        assertThat(peakActiveTasks.get(), is(lessThanOrEqualTo(threadCount)));
        assertThat(peakActiveTasks.get(), is(greaterThan(0)));
    }

    @Test
    void executeFailFast_SingleThreadLimit_ExecutesSequentially() throws ExecutionException {
        int threadCount = 1;
        int taskCount = 20;

        AtomicInteger activeTasks = new AtomicInteger();
        AtomicInteger peakActiveTasks = new AtomicInteger();
        AtomicInteger executed = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            tasks.add(() -> {
                int active = activeTasks.incrementAndGet();
                peakActiveTasks.accumulateAndGet(active, Math::max);

                try {
                    Thread.sleep(10);
                    executed.incrementAndGet();
                } finally {
                    activeTasks.decrementAndGet();
                }

                return null;
            });
        }

        taskService.executeFailFast(tasks, threadCount, executorService);

        assertThat(executed.get(), is(taskCount));
        assertThat(peakActiveTasks.get(), is(1));
    }

    @Test
    void executeFailFast_Success() throws ExecutionException {
        int threadCount = 2;
        int taskCount = 10;

        AtomicInteger counter = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            tasks.add(() -> {
                counter.incrementAndGet();
                return null;
            });
        }

        taskService.executeFailFast(tasks, threadCount, executorService);

        assertThat(counter.get(), is(taskCount));
    }

    @Test
    void executeFailFast_Failure_InterruptsAllRunningTasks() throws Exception {
        int threadCount = 4;

        CountDownLatch allTasksStarted = new CountDownLatch(threadCount);
        CountDownLatch interruptedTasksLatch = new CountDownLatch(threadCount - 1);
        AtomicInteger interruptedTasks = new AtomicInteger();

        Callable<Void> failingTask = () -> {
            allTasksStarted.countDown();

            if (!allTasksStarted.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Not all tasks started");
            }

            throw new RuntimeException("Boom!");
        };

        Callable<Void> longTask = () -> {
            allTasksStarted.countDown();

            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                interruptedTasks.incrementAndGet();
                interruptedTasksLatch.countDown();
                Thread.currentThread().interrupt();
            }

            return null;
        };

        List<Callable<Void>> tasks = List.of(
                failingTask,
                longTask,
                longTask,
                longTask
        );

        ExecutionException exception = assertThrows(
                ExecutionException.class,
                () -> taskService.executeFailFast(tasks, threadCount, executorService)
        );

        assertThat(exception.getCause(), is(instanceOf(RuntimeException.class)));
        assertThat(exception.getCause().getMessage(), is("Boom!"));

        assertThat(
                "All other running tasks must be interrupted",
                interruptedTasksLatch.await(2, TimeUnit.SECONDS),
                is(true)
        );

        assertThat(interruptedTasks.get(), is(3));
    }

    @Test
    void executeFailFast_OutOfMemoryError_InterruptsRunningTasksAndPropagatesError() throws Exception {
        int threadCount = 2;

        CountDownLatch allTasksStarted = new CountDownLatch(threadCount);
        CountDownLatch interruptedTaskLatch = new CountDownLatch(1);
        AtomicBoolean runningTaskInterrupted = new AtomicBoolean();

        Callable<Void> oomTask = () -> {
            allTasksStarted.countDown();

            if (!allTasksStarted.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Not all tasks started");
            }

            throw new OutOfMemoryError("Simulated OOM");
        };

        Callable<Void> runningTask = () -> {
            allTasksStarted.countDown();

            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                runningTaskInterrupted.set(true);
                interruptedTaskLatch.countDown();
                Thread.currentThread().interrupt();
            }

            return null;
        };

        List<Callable<Void>> tasks = List.of(oomTask, runningTask);

        Throwable thrown = assertThrows(
                Throwable.class,
                () -> taskService.executeFailFast(tasks, threadCount, executorService)
        );

        assertThat(thrown, is(instanceOf(OutOfMemoryError.class)));
        assertThat(thrown.getMessage(), is("Simulated OOM"));

        assertThat(
                "Running task must be interrupted due to OOM fail-fast",
                interruptedTaskLatch.await(2, TimeUnit.SECONDS),
                is(true)
        );

        assertThat(runningTaskInterrupted.get(), is(true));
    }

    @Test
    void executeFailFast_OutOfMemoryError_QueuedTasksAreSkipped() throws Exception {
        int threadCount = 2;

        CountDownLatch runningStarted = new CountDownLatch(threadCount);
        CountDownLatch releaseOom = new CountDownLatch(1);
        AtomicInteger queuedExecuted = new AtomicInteger();

        Callable<Void> oomTask = () -> {
            runningStarted.countDown();

            if (!releaseOom.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("OOM release was not triggered");
            }

            throw new OutOfMemoryError("Simulated OOM in queue test");
        };

        Callable<Void> runningTask = () -> {
            runningStarted.countDown();

            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return null;
        };

        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(oomTask);
        tasks.add(runningTask);

        for (int i = 0; i < 20; i++) {
            tasks.add(() -> {
                queuedExecuted.incrementAndGet();
                return null;
            });
        }

        AtomicReference<Throwable> thrownInCaller = new AtomicReference<>();
        CountDownLatch callerFinished = new CountDownLatch(1);

        Thread callerThread = new Thread(() -> {
            try {
                taskService.executeFailFast(tasks, threadCount, executorService);
            } catch (Throwable throwable) {
                thrownInCaller.set(throwable);
            } finally {
                callerFinished.countDown();
            }
        });

        callerThread.start();

        assertThat(runningStarted.await(2, TimeUnit.SECONDS), is(true));

        releaseOom.countDown();

        assertThat(callerFinished.await(2, TimeUnit.SECONDS), is(true));

        Throwable thrown = thrownInCaller.get();
        assertThat(thrown, is(instanceOf(OutOfMemoryError.class)));
        assertThat(queuedExecuted.get(), is(0));
    }

    @Test
    void executeFailFast_Failure_QueuedTasksAreSkipped() throws Exception {
        int threadCount = 4;

        CountDownLatch runningStarted = new CountDownLatch(threadCount);
        CountDownLatch releaseFailure = new CountDownLatch(1);
        AtomicInteger queuedExecuted = new AtomicInteger();

        Callable<Void> failingTask = () -> {
            runningStarted.countDown();

            if (!releaseFailure.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Failure release was not triggered");
            }

            throw new RuntimeException("Boom!");
        };

        Callable<Void> runningTask = () -> {
            runningStarted.countDown();

            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return null;
        };

        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(failingTask);

        for (int i = 1; i < threadCount; i++) {
            tasks.add(runningTask);
        }

        for (int i = 0; i < 20; i++) {
            tasks.add(() -> {
                queuedExecuted.incrementAndGet();
                return null;
            });
        }

        AtomicReference<Throwable> thrownInCaller = new AtomicReference<>();
        CountDownLatch callerFinished = new CountDownLatch(1);

        Thread callerThread = new Thread(() -> {
            try {
                taskService.executeFailFast(tasks, threadCount, executorService);
            } catch (Throwable throwable) {
                thrownInCaller.set(throwable);
            } finally {
                callerFinished.countDown();
            }
        });

        callerThread.start();

        assertThat(runningStarted.await(2, TimeUnit.SECONDS), is(true));

        releaseFailure.countDown();

        assertThat(callerFinished.await(2, TimeUnit.SECONDS), is(true));

        assertThat(thrownInCaller.get(), is(instanceOf(ExecutionException.class)));
        assertThat(queuedExecuted.get(), is(0));
    }

    @Test
    void executeFailFast_FailureInLaterTask_StopsRemainingTasks() throws Exception {
        int threadCount = 2;

        CountDownLatch firstBatchStarted = new CountDownLatch(2);
        CountDownLatch releaseFirstTask = new CountDownLatch(1);

        AtomicInteger laterTasksExecuted = new AtomicInteger();

        Callable<Void> firstTask = () -> {
            firstBatchStarted.countDown();

            if (!releaseFirstTask.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("First task was not released");
            }

            return null;
        };

        Callable<Void> secondTask = () -> {
            firstBatchStarted.countDown();

            if (!firstBatchStarted.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("First batch did not start");
            }

            Thread.sleep(10_000);
            return null;
        };

        Callable<Void> failingTask = () -> {
            throw new RuntimeException("Boom in later task!");
        };

        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(firstTask);
        tasks.add(secondTask);
        tasks.add(failingTask);

        for (int i = 0; i < 10; i++) {
            tasks.add(() -> {
                laterTasksExecuted.incrementAndGet();
                return null;
            });
        }

        Thread callerThread = new Thread(() -> {
            try {
                taskService.executeFailFast(
                        tasks,
                        threadCount,
                        executorService
                );
            } catch (ExecutionException _) {
            }
        });

        callerThread.start();

        assertThat(firstBatchStarted.await(2, TimeUnit.SECONDS), is(true));

        releaseFirstTask.countDown();

        callerThread.join(2_000);

        assertThat(callerThread.isAlive(), is(false));
        assertThat(laterTasksExecuted.get(), is(0));
    }

    @Test
    void executeFailFast_MultipleConcurrentFailures_PropagatesOneException() throws ExecutionException {
        int threadCount = 4;

        CountDownLatch allStarted = new CountDownLatch(threadCount);

        List<Callable<Void>> tasks = List.of(
                failingTask("error-1", allStarted),
                failingTask("error-2", allStarted),
                failingTask("error-3", allStarted),
                failingTask("error-4", allStarted)
        );

        ExecutionException exception = assertThrows(
                ExecutionException.class,
                () -> taskService.executeFailFast(
                        tasks,
                        threadCount,
                        executorService
                )
        );

        assertThat(exception.getCause(), is(instanceOf(RuntimeException.class)));
        assertThat(
                exception.getCause().getMessage(),
                anyOf(
                        is("error-1"),
                        is("error-2"),
                        is("error-3"),
                        is("error-4")
                )
        );
    }

    @Test
    void executeFailFast_Failure_ReturnsBeforeStubbornTaskFinishes() throws Exception {
        int threadCount = 2;

        CountDownLatch stubbornTaskStarted = new CountDownLatch(1);
        CountDownLatch allowStubbornTaskToFinish = new CountDownLatch(1);
        CountDownLatch stubbornTaskFinished = new CountDownLatch(1);

        Callable<Void> stubbornTask = () -> {
            stubbornTaskStarted.countDown();

            while (allowStubbornTaskToFinish.getCount() > 0) {
                try {
                    allowStubbornTaskToFinish.await(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException _) {
                }
            }

            stubbornTaskFinished.countDown();
            return null;
        };

        Callable<Void> failingTask = () -> {
            if (!stubbornTaskStarted.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Stubborn task did not start");
            }

            throw new RuntimeException("Boom!");
        };

        AtomicReference<Throwable> thrownInCaller = new AtomicReference<>();
        CountDownLatch callerFinished = new CountDownLatch(1);

        Thread callerThread = new Thread(() -> {
            try {
                taskService.executeFailFast(
                        List.of(stubbornTask, failingTask),
                        threadCount,
                        executorService
                );
            } catch (Throwable throwable) {
                thrownInCaller.set(throwable);
            } finally {
                callerFinished.countDown();
            }
        });

        callerThread.start();

        assertThat(callerFinished.await(2, TimeUnit.SECONDS), is(true));

        assertThat(thrownInCaller.get(), is(instanceOf(ExecutionException.class)));
        assertThat(thrownInCaller.get().getCause(), is(instanceOf(RuntimeException.class)));
        assertThat(thrownInCaller.get().getCause().getMessage(), is("Boom!"));

        assertThat(
                "Fail-fast must not wait for a task that ignores interruption",
                stubbornTaskFinished.getCount(),
                is(greaterThan(0L))
        );

        allowStubbornTaskToFinish.countDown();

        assertThat(
                stubbornTaskFinished.await(2, TimeUnit.SECONDS),
                is(true)
        );
    }

    @Test
    void executeFailFast_ExecutorRejectsTask_CancelsRunningTasks() throws Exception {
        int threadCount = 2;

        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch runningTaskInterrupted = new CountDownLatch(1);
        AtomicInteger submissions = new AtomicInteger();

        Callable<Void> runningTask = () -> {
            taskStarted.countDown();

            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                runningTaskInterrupted.countDown();
                Thread.currentThread().interrupt();
            }

            return null;
        };

        ExecutorService rejectingExecutor = new TrackingExecutorService(10) {
            @Override
            public Future<?> submit(Runnable task) {
                int submission = submissions.incrementAndGet();

                if (submission > 1) {
                    throw new RejectedExecutionException("Submission rejected");
                }

                Future<?> future = super.submit(task);

                try {
                    if (!taskStarted.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Task did not start");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for task start", e);
                }

                return future;
            }
        };

        try {
            ExecutionException exception = assertThrows(
                    ExecutionException.class,
                    () -> taskService.executeFailFast(
                            List.of(runningTask, () -> null),
                            threadCount,
                            rejectingExecutor
                    )
            );

            assertThat(
                    exception.getCause(),
                    is(instanceOf(RejectedExecutionException.class))
            );

            assertThat(
                    "Running task must be interrupted after submission rejection",
                    runningTaskInterrupted.await(2, TimeUnit.SECONDS),
                    is(true)
            );
        } finally {
            rejectingExecutor.shutdownNow();
        }
    }

    @Test
    void executeFailFast_CallerInterruptedWhileWaitingForSlot() throws Exception {
        int threadCount = 1;

        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch releaseTask = new CountDownLatch(1);

        AtomicReference<Throwable> thrownInCaller = new AtomicReference<>();
        AtomicBoolean interruptRestored = new AtomicBoolean();
        CountDownLatch callerFinished = new CountDownLatch(1);

        Callable<Void> blockingTask = () -> {
            taskStarted.countDown();

            try {
                releaseTask.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return null;
        };

        Thread callerThread = new Thread(() -> {
            try {
                taskService.executeFailFast(
                        List.of(blockingTask, () -> null),
                        threadCount,
                        executorService
                );
            } catch (Throwable throwable) {
                thrownInCaller.set(throwable);
                interruptRestored.set(Thread.currentThread().isInterrupted());
            } finally {
                callerFinished.countDown();
            }
        });

        callerThread.start();

        assertThat(taskStarted.await(2, TimeUnit.SECONDS), is(true));

        callerThread.interrupt();

        assertThat(callerFinished.await(2, TimeUnit.SECONDS), is(true));

        releaseTask.countDown();

        assertThat(thrownInCaller.get(), is(instanceOf(ExecutionException.class)));
        assertThat(
                thrownInCaller.get().getCause(),
                is(instanceOf(InterruptedException.class))
        );
        assertThat(interruptRestored.get(), is(true));
    }

    @Test
    void executeFailFast_CallerInterruptedWhileWaitingForTask() throws Exception {
        int threadCount = 1;

        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch releaseTask = new CountDownLatch(1);

        AtomicReference<Throwable> thrownInCaller = new AtomicReference<>();
        AtomicBoolean interruptRestored = new AtomicBoolean();
        CountDownLatch callerFinished = new CountDownLatch(1);

        Callable<Void> blockingTask = () -> {
            taskStarted.countDown();

            try {
                releaseTask.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return null;
        };

        Thread callerThread = new Thread(() -> {
            try {
                taskService.executeFailFast(
                        List.of(blockingTask),
                        threadCount,
                        executorService
                );
            } catch (Throwable throwable) {
                thrownInCaller.set(throwable);
                interruptRestored.set(Thread.currentThread().isInterrupted());
            } finally {
                callerFinished.countDown();
            }
        });

        callerThread.start();

        assertThat(taskStarted.await(2, TimeUnit.SECONDS), is(true));

        callerThread.interrupt();

        assertThat(callerFinished.await(2, TimeUnit.SECONDS), is(true));

        releaseTask.countDown();

        assertThat(thrownInCaller.get(), is(instanceOf(ExecutionException.class)));
        assertThat(
                thrownInCaller.get().getCause(),
                is(instanceOf(InterruptedException.class))
        );
        assertThat(interruptRestored.get(), is(true));
    }

    @Test
    void executeFailFast_ExecutorShutdownNow_CancelsAllTasks() throws Exception {
        int threadCount = 2;

        CountDownLatch tasksStarted = new CountDownLatch(2);
        AtomicInteger interruptedTasksCount = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> {
                tasksStarted.countDown();

                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    interruptedTasksCount.incrementAndGet();
                    Thread.currentThread().interrupt();
                }

                return null;
            });
        }

        AtomicReference<Throwable> thrownInCaller = new AtomicReference<>();
        CountDownLatch callerFinished = new CountDownLatch(1);

        Thread callerThread = new Thread(() -> {
            try {
                taskService.executeFailFast(
                        tasks,
                        threadCount,
                        executorService
                );
            } catch (Throwable throwable) {
                thrownInCaller.set(throwable);
            } finally {
                callerFinished.countDown();
            }
        });

        callerThread.start();

        assertThat(tasksStarted.await(2, TimeUnit.SECONDS), is(true));

        executorService.shutdownNow();

        assertThat(
                callerFinished.await(2, TimeUnit.SECONDS),
                is(true)
        );

        assertThat(
                thrownInCaller.get(),
                is(instanceOf(ExecutionException.class))
        );

        assertThat(
                interruptedTasksCount.get(),
                is(greaterThan(0))
        );
    }

    @Test
    void executeFailFast_InterruptedTask_RestoresCallerInterruptStatus() {
        int threadCount = 2;

        Callable<Void> taskThrowingInterrupted = () -> {
            throw new InterruptedException("Simulated interrupt");
        };

        ExecutionException exception = assertThrows(
                ExecutionException.class,
                () -> taskService.executeFailFast(
                        List.of(taskThrowingInterrupted),
                        threadCount,
                        executorService
                )
        );

        assertThat(
                exception.getCause(),
                is(instanceOf(InterruptedException.class))
        );

        assertThat(
                "Caller thread interrupt status must be restored",
                Thread.currentThread().isInterrupted(),
                is(true)
        );
    }

    @Test
    void executeFailFast_InvalidThreadCount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> taskService.executeFailFast(
                        List.of(() -> null),
                        0,
                        executorService
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> taskService.executeFailFast(
                        List.of(() -> null),
                        -1,
                        executorService
                )
        );
    }

    @Test
    void executeFailFast_EmptyList() throws ExecutionException {
        taskService.executeFailFast(
                Collections.emptyList(),
                2,
                executorService
        );
    }

    private Callable<Void> failingTask(
            String message,
            CountDownLatch allStarted
    ) {
        return () -> {
            allStarted.countDown();

            if (!allStarted.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Not all tasks started");
            }

            throw new RuntimeException(message);
        };
    }

    private static class TrackingExecutorService extends ThreadPoolExecutor {

        private final AtomicInteger activeTasks = new AtomicInteger();
        private final AtomicInteger peakActiveTasks = new AtomicInteger();

        TrackingExecutorService(int poolSize) {
            super(
                    poolSize,
                    poolSize,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>()
            );
        }

        @Override
        protected void beforeExecute(Thread thread, Runnable runnable) {
            super.beforeExecute(thread, runnable);

            int currentActive = activeTasks.incrementAndGet();
            peakActiveTasks.accumulateAndGet(currentActive, Math::max);
        }

        @Override
        protected void afterExecute(Runnable runnable, Throwable throwable) {
            try {
                super.afterExecute(runnable, throwable);
            } finally {
                activeTasks.decrementAndGet();
            }
        }

        int getPeakActiveTasks() {
            return peakActiveTasks.get();
        }
    }

}