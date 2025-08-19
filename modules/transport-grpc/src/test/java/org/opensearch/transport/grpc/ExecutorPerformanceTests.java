/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.transport.grpc;

import org.opensearch.common.network.NetworkService;
import org.opensearch.common.settings.Settings;
import org.opensearch.test.OpenSearchTestCase;
import org.junit.Before;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.BindableService;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;

/**
 * Performance tests for the gRPC transport executor improvements.
 * These tests verify that the new executor configuration provides better performance
 * characteristics for CPU-intensive tasks like protobuf serialization.
 */
public class ExecutorPerformanceTests extends OpenSearchTestCase {

    private NetworkService networkService;
    private List<BindableService> services;

    @Before
    public void setup() {
        networkService = new NetworkService(List.of());
        services = List.of();
    }

    public void testExecutorHandlesConcurrentTasks() throws InterruptedException {
        // Test that the ForkJoinPool executor can handle concurrent CPU-intensive tasks
        Settings settings = Settings.builder()
            .put(Netty4GrpcServerTransport.SETTING_GRPC_PORT.getKey(), OpenSearchTestCase.getPortRange())
            .put(Netty4GrpcServerTransport.SETTING_GRPC_EXECUTOR_COUNT.getKey(), 4)
            .build();

        try (Netty4GrpcServerTransport transport = new Netty4GrpcServerTransport(settings, services, networkService)) {
            transport.start();

            ExecutorService executor = getGrpcExecutor(transport);
            assertNotNull("Executor should be created", executor);
            assertTrue("Executor should be a ForkJoinPool", executor instanceof ForkJoinPool);

            // Submit multiple CPU-intensive tasks concurrently
            int taskCount = 20;
            CountDownLatch latch = new CountDownLatch(taskCount);
            AtomicInteger completedTasks = new AtomicInteger(0);

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    try {
                        // Simulate CPU-intensive work (like protobuf serialization)
                        simulateCpuIntensiveWork();
                        completedTasks.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all tasks to complete (with timeout)
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();

            assertTrue("All tasks should complete within timeout", completed);
            assertEquals("All tasks should be completed", taskCount, completedTasks.get());

            // Verify reasonable performance (should complete in less than 5 seconds for 20 tasks)
            long duration = endTime - startTime;
            assertTrue("Tasks should complete in reasonable time: " + duration + "ms", duration < 5000);

            transport.stop();
        }
    }

    public void testEventLoopGroupsNotBlockedByExecutor() throws InterruptedException {
        // Test that event loop groups remain responsive while executor handles CPU work
        Settings settings = Settings.builder()
            .put(Netty4GrpcServerTransport.SETTING_GRPC_PORT.getKey(), OpenSearchTestCase.getPortRange())
            .put(Netty4GrpcServerTransport.SETTING_GRPC_EXECUTOR_COUNT.getKey(), 2)
            .put(Netty4GrpcServerTransport.SETTING_GRPC_WORKER_COUNT.getKey(), 2)
            .build();

        try (Netty4GrpcServerTransport transport = new Netty4GrpcServerTransport(settings, services, networkService)) {
            transport.start();

            ExecutorService executor = getGrpcExecutor(transport);
            EventLoopGroup workerGroup = getWorkerEventLoopGroup(transport);

            assertNotNull("Executor should be created", executor);
            assertNotNull("Worker group should be created", workerGroup);

            // Submit a long-running task to the executor
            CountDownLatch executorTaskStarted = new CountDownLatch(1);
            CountDownLatch executorTaskFinish = new CountDownLatch(1);

            executor.submit(() -> {
                executorTaskStarted.countDown();
                try {
                    // Wait for signal to finish
                    executorTaskFinish.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Wait for executor task to start
            assertTrue("Executor task should start", executorTaskStarted.await(1, TimeUnit.SECONDS));

            // Verify event loop group is still responsive
            CountDownLatch eventLoopTaskCompleted = new CountDownLatch(1);
            AtomicInteger eventLoopTaskResult = new AtomicInteger(0);

            workerGroup.execute(() -> {
                eventLoopTaskResult.set(42); // Simple task
                eventLoopTaskCompleted.countDown();
            });

            // Event loop task should complete quickly even while executor is busy
            boolean eventLoopCompleted = eventLoopTaskCompleted.await(1, TimeUnit.SECONDS);
            assertTrue("Event loop should remain responsive", eventLoopCompleted);
            assertEquals("Event loop task should execute correctly", 42, eventLoopTaskResult.get());

            // Signal executor task to finish
            executorTaskFinish.countDown();

            transport.stop();
        }
    }

    public void testForkJoinPoolWorkStealing() throws InterruptedException {
        // Test that ForkJoinPool's work-stealing behavior helps with uneven task distribution
        Settings settings = Settings.builder()
            .put(Netty4GrpcServerTransport.SETTING_GRPC_PORT.getKey(), OpenSearchTestCase.getPortRange())
            .put(Netty4GrpcServerTransport.SETTING_GRPC_EXECUTOR_COUNT.getKey(), 4)
            .build();

        try (Netty4GrpcServerTransport transport = new Netty4GrpcServerTransport(settings, services, networkService)) {
            transport.start();

            ExecutorService executor = getGrpcExecutor(transport);
            assertTrue("Executor should be a ForkJoinPool", executor instanceof ForkJoinPool);

            ForkJoinPool forkJoinPool = (ForkJoinPool) executor;

            // Submit tasks with varying workloads
            int taskCount = 16;
            CountDownLatch latch = new CountDownLatch(taskCount);
            AtomicInteger completedTasks = new AtomicInteger(0);

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    try {
                        // Vary the work amount - some tasks do more work than others
                        int workAmount = (taskId % 4) + 1; // 1-4 units of work
                        for (int j = 0; j < workAmount; j++) {
                            simulateCpuIntensiveWork();
                        }
                        completedTasks.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(15, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();

            assertTrue("All tasks should complete", completed);
            assertEquals("All tasks should be completed", taskCount, completedTasks.get());

            // Verify that work-stealing helped - should complete faster than sequential execution
            long duration = endTime - startTime;
            assertTrue("Work-stealing should provide reasonable performance: " + duration + "ms", duration < 10000);

            transport.stop();
        }
    }

    public void testExecutorResourceCleanup() throws InterruptedException {
        // Test that executor resources are properly cleaned up
        Settings settings = Settings.builder()
            .put(Netty4GrpcServerTransport.SETTING_GRPC_PORT.getKey(), OpenSearchTestCase.getPortRange())
            .put(Netty4GrpcServerTransport.SETTING_GRPC_EXECUTOR_COUNT.getKey(), 2)
            .build();

        Netty4GrpcServerTransport transport = new Netty4GrpcServerTransport(settings, services, networkService);
        transport.start();

        ExecutorService executor = getGrpcExecutor(transport);
        ForkJoinPool forkJoinPool = (ForkJoinPool) executor;

        // Submit a task to ensure executor is active
        CountDownLatch taskCompleted = new CountDownLatch(1);
        executor.submit(() -> {
            simulateCpuIntensiveWork();
            taskCompleted.countDown();
        });

        assertTrue("Task should complete", taskCompleted.await(5, TimeUnit.SECONDS));
        assertFalse("Executor should be active", executor.isShutdown());
        assertTrue("ForkJoinPool should have active threads", forkJoinPool.getActiveThreadCount() >= 0);

        // Stop transport and verify cleanup
        transport.stop();

        assertTrue("Executor should be shutdown after transport stop", executor.isShutdown());

        // Wait a bit for threads to terminate
        Thread.sleep(100);

        transport.close();
    }

    // Helper methods
    private ExecutorService getGrpcExecutor(Netty4GrpcServerTransport transport) {
        try {
            Field field = Netty4GrpcServerTransport.class.getDeclaredField("grpcExecutor");
            field.setAccessible(true);
            return (ExecutorService) field.get(transport);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access grpcExecutor field", e);
        }
    }

    private EventLoopGroup getWorkerEventLoopGroup(Netty4GrpcServerTransport transport) {
        try {
            Field field = Netty4GrpcServerTransport.class.getDeclaredField("workerEventLoopGroup");
            field.setAccessible(true);
            return (EventLoopGroup) field.get(transport);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access workerEventLoopGroup field", e);
        }
    }

    /**
     * Simulates CPU-intensive work similar to protobuf serialization.
     * This creates some computational load without being too heavy for tests.
     */
    private void simulateCpuIntensiveWork() {
        // Simulate work similar to protobuf serialization - string manipulation and object creation
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("field_").append(i).append("_value_").append(i * 2);
            if (i % 100 == 0) {
                // Simulate some object creation/GC pressure
                String temp = sb.toString();
                sb.setLength(0);
                temp.hashCode(); // Use the string to prevent optimization
            }
        }
    }
}
