package com.scheduler;

// Represents a task with a given priority. Implements Comparable to allow ordering in a PriorityQueue.
public class Job implements Comparable<Job> {
    private final String name;
    private final int priority;
    private final Runnable task;

    public Job(String name, int priority, Runnable task) {
        this.name = name;
        this.priority = priority;
        this.task = task;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }
    
    public void run() {
        System.out.println("Executing Job: " + name + " (Priority: " + priority + ") on thread " + Thread.currentThread().getName());
        task.run();
        System.out.println("Finished Job: " + name);
    }

    // Compares jobs based on priority. Lower number = higher priority.
    @Override
    public int compareTo(Job other) {
        return Integer.compare(this.priority, other.priority);
    }
}
