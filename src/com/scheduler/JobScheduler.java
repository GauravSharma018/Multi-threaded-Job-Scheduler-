package com.scheduler;

import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// Manages a pool of worker threads and a priority queue of jobs.
public class JobScheduler {
    private final PriorityQueue<Job> jobQueue = new PriorityQueue<>();
    private final ReentrantLock queueLock = new ReentrantLock();
    private final Condition newJobCondition = queueLock.newCondition();
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final WorkerThread[] workerThreads;

    public JobScheduler(int poolSize) {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("Thread pool size must be positive.");
        }
        workerThreads = new WorkerThread[poolSize];
        for (int i = 0; i < poolSize; i++) {
            workerThreads[i] = new WorkerThread("Worker-" + (i + 1));
            workerThreads[i].start();
        }
        System.out.println(poolSize + " worker threads started.");
    }

    // Submits a new job to the scheduler.
    public void submit(Job job) {
        if (isShutdown.get()) {
            System.out.println("Scheduler is shut down. Cannot accept new jobs.");
            return;
        }
        queueLock.lock();
        try {
            jobQueue.add(job);
            System.out.println("New Job Submitted: " + job.getName() + " (Priority: " + job.getPriority() + ")");
            // Signal one waiting worker thread that a new job is available.
            newJobCondition.signal();
        } finally {
            queueLock.unlock();
        }
    }

    // Shuts down the scheduler and all worker threads.
    public void shutdown() {
        isShutdown.set(true);
        System.out.println("Scheduler shutting down...");
        queueLock.lock();
        try {
            // Signal all waiting threads to wake up and check the shutdown flag.
            newJobCondition.signalAll();
        } finally {
            queueLock.unlock();
        }

        // Wait for all worker threads to terminate.
        for (WorkerThread worker : workerThreads) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("All worker threads have shut down.");
    }

    // Inner class representing a worker thread from the pool.
    private class WorkerThread extends Thread {
        public WorkerThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (!isShutdown.get()) {
                Job job;
                queueLock.lock();
                try {
                    // Wait for a job to become available.
                    while (jobQueue.isEmpty() && !isShutdown.get()) {
                        try {
                            newJobCondition.await();
                        } catch (InterruptedException e) {
                            if (isShutdown.get()) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                    }

                    // If shutdown was called while waiting, exit the loop.
                    if (isShutdown.get()) {
                        break;
                    }

                    // Poll the highest-priority job from the queue.
                    job = jobQueue.poll();

                } finally {
                    queueLock.unlock();
                }

                // If a job was retrieved, execute it outside the lock.
                if (job != null) {
                    try {
                        job.run();
                    } catch (Exception e) {
                        System.err.println("Error executing job: " + job.getName());
                        e.printStackTrace();
                    }
                }
            }
            System.out.println(getName() + " is terminating.");
        }
    }
}
