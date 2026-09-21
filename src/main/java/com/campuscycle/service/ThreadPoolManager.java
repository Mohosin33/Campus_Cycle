package com.campuscycle.service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Thread Pool Manager managing concurrency across the application.
 * Demonstrates Multi-threading and Thread Pools (Assignment Requirement).
 */
public class ThreadPoolManager {
    private static final Logger LOGGER = Logger.getLogger(ThreadPoolManager.class.getName());
    private static ThreadPoolManager instance;
    private final ExecutorService executorService;

    private ThreadPoolManager() {
        int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
        AtomicInteger threadCount = new AtomicInteger(1);

        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r, "CampusCycle-Worker-" + threadCount.getAndIncrement());
            thread.setDaemon(true); // Allow JVM to exit gracefully
            return thread;
        };

        this.executorService = new ThreadPoolExecutor(
            corePoolSize,
            corePoolSize * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        LOGGER.info("Initialized Concurrency Thread Pool with " + corePoolSize + " worker threads.");
    }

    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void execute(Runnable task) {
        executorService.execute(task);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }

    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
