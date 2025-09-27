package com.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int numThreads = 4;
        int numJobs = 10;
        
        // --- Asynchronous Execution (Multi-threaded Scheduler) ---
        System.out.println("--- Starting Asynchronous Job Execution (Multi-threaded) ---");
        JobScheduler scheduler = new JobScheduler(numThreads);
        CountDownLatch latch = new CountDownLatch(numJobs);

        // A helper task that simulates work
        Runnable task = () -> {
            try {
                Thread.sleep(1000); // Simulate 1 second of work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        };

        // Submit jobs in a non-priority order
        System.out.println("\nSubmitting " + numJobs + " jobs with varying priorities...");
        scheduler.submit(new Job("Job A", 5, task));
        scheduler.submit(new Job("Job B", 1, task)); // Highest priority
        scheduler.submit(new Job("Job C", 3, task));
        scheduler.submit(new Job("Job D", 10, task)); // Lowest priority
        scheduler.submit(new Job("Job E", 2, task));
        scheduler.submit(new Job("Job F", 3, task));
        scheduler.submit(new Job("Job G", 1, task)); // Highest priority
        scheduler.submit(new Job("Job H", 5, task));
        scheduler.submit(new Job("Job I", 8, task));
        scheduler.submit(new Job("Job J", 2, task));

        long asyncStartTime = System.currentTimeMillis();
        
        latch.await(); // Wait for all jobs to complete
        scheduler.shutdown();
        
        long asyncEndTime = System.currentTimeMillis();
        long asyncDuration = asyncEndTime - asyncStartTime;
        System.out.println("--- Asynchronous Execution Finished ---");


        // --- Synchronous Execution (Single-threaded) ---
        System.out.println("\n\n--- Starting Synchronous Job Execution (Single-threaded) ---");
        PriorityQueue<Job> syncQueue = new PriorityQueue<>();

        // Helper task for the synchronous test
        Runnable syncTask = () -> {
            try {
                Thread.sleep(1000); // Simulate 1 second of work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        // Add the same jobs to a simple priority queue
        syncQueue.add(new Job("Job A", 5, syncTask));
        syncQueue.add(new Job("Job B", 1, syncTask));
        syncQueue.add(new Job("Job C", 3, syncTask));
        syncQueue.add(new Job("Job D", 10, syncTask));
        syncQueue.add(new Job("Job E", 2, syncTask));
        syncQueue.add(new Job("Job F", 3, syncTask));
        syncQueue.add(new Job("Job G", 1, syncTask));
        syncQueue.add(new Job("Job H", 5, syncTask));
        syncQueue.add(new Job("Job I", 8, syncTask));
        syncQueue.add(new Job("Job J", 2, syncTask));
        
        long syncStartTime = System.currentTimeMillis();
        
        // Execute jobs one by one from the priority queue
        while (!syncQueue.isEmpty()) {
            Job job = syncQueue.poll();
            job.run();
        }
        
        long syncEndTime = System.currentTimeMillis();
        long syncDuration = syncEndTime - syncStartTime;
        System.out.println("--- Synchronous Execution Finished ---");


        // --- Benchmark Summary ---
        System.out.println("\n\n--- Benchmark Summary ---");
        System.out.println("Number of Jobs: " + numJobs);
        System.out.println("Number of Threads: " + numThreads);
        System.out.println("Synchronous (Single-threaded) Execution Time: " + syncDuration + " ms");
        System.out.println("Asynchronous (Multi-threaded) Execution Time: " + asyncDuration + " ms");

        if (asyncDuration > 0) {
            double speedup = (double) syncDuration / asyncDuration;
            System.out.printf("Performance Gain (Speedup): %.2fx%n", speedup);
        }
    }
}
