# Java Multi-threaded Job Scheduler

This project is a high-performance, multi-threaded job scheduler built in Java. It demonstrates core concepts of concurrent programming by managing a queue of tasks with varying priority levels and executing them in parallel using a custom thread pool.

The scheduler ensures that high-priority jobs are always executed before lower-priority jobs, showcasing an efficient implementation of a priority-based task management system.

---
## ✨ Core Concepts Demonstrated

* **Priority Scheduling**: Uses a **`java.util.PriorityQueue`** (a Min-Heap) to store jobs, ensuring that the job with the highest priority (lowest integer value) is always at the front of the queue.
* **Thread Pool**: Implements a fixed-size pool of worker threads to execute jobs concurrently, maximizing CPU utilization.
* **Producer-Consumer Pattern**: The `submit` method acts as a "producer," adding jobs to the shared queue. The worker threads act as "consumers," pulling jobs from the queue to execute them.
* **Synchronization Primitives**: Employs advanced concurrency controls for thread-safe operations:
    * **`ReentrantLock`**: Provides exclusive access to the shared job queue, preventing race conditions.
    * **`Condition` Variables**: Allows worker threads to efficiently wait (`await`) when the queue is empty and be woken up (`signal`) only when a new job is added, preventing busy-waiting and conserving CPU cycles.
    * **`AtomicBoolean`**: A lock-free variable used for a graceful shutdown signal.

---
## ⚙️ How It Works

1.  **Initialization**: The `JobScheduler` is initialized with a fixed number of worker threads, which immediately start and wait for tasks.
2.  **Job Submission**: When a new `Job` is submitted, the scheduler locks the queue, adds the job (which the `PriorityQueue` automatically places in the correct position), and then signals one of the waiting worker threads.
3.  **Job Execution**: A woken worker thread locks the queue, retrieves the highest-priority job (`poll`), and releases the lock. It then executes the job's task *outside* the locked section, allowing other workers to access the queue simultaneously.
4.  **Shutdown**: The `shutdown` method sets a flag, wakes up all threads, and waits for them to finish their current tasks and terminate gracefully.

## Of course. Let's do a deep dive into the Java Multi-threaded Job Scheduler project.

This explanation covers the project's purpose, the core computer science concepts it demonstrates, a detailed breakdown of the architecture, and the rationale behind key implementation details.

-----

## Project Overview and Goal 🎯

The **Multi-threaded Job Scheduler** is a system designed to manage and execute a series of tasks, or "jobs," concurrently. Its primary goal is to solve two common problems in software:

1.  **Inefficiency**: Running tasks one after another (synchronously) is slow and wastes system resources, especially on modern multi-core processors.
2.  **Prioritization**: Not all tasks are equally important. A critical task (like processing a payment) should not have to wait for a low-priority task (like generating a weekly report).

This project addresses these issues by using a **thread pool** to run jobs in parallel and a **priority queue** to ensure that the most important jobs are always executed first. It's a foundational model for systems like background job processors in web applications, task runners in operating systems, or data processing pipelines.

-----

## Core Concepts Explained 🧠

This project is a practical application of several fundamental computer science concepts. Understanding them is key to understanding the project.

#### **Thread Pool**

A thread pool is a group of pre-initialized worker threads that stand ready to execute tasks.

  * **Problem it solves**: Creating a new thread for every single task is computationally expensive and slow.
  * **Analogy**: Imagine a supermarket with a fixed number of cashiers (threads). When a customer (a job) arrives, they go to an available cashier. This is far more efficient than hiring a new cashier for every single customer and firing them immediately after. Our `JobScheduler` creates a fixed number of `WorkerThread`s at the start to form this pool.

#### **Priority Queue (Min-Heap)**

This is a special type of queue where each element has a priority. When you retrieve an element, you are always given the one with the highest priority, regardless of when it was added.

  * **Problem it solves**: It ensures that critical tasks are not stuck behind non-critical ones.
  * **Analogy**: Think of a hospital emergency room. Patients are not treated in the order they arrive (like a regular queue). They are treated based on the severity of their condition (priority). Our `jobQueue` uses a `java.util.PriorityQueue`, which is implemented as a min-heap, to automatically handle this logic.

#### **Producer-Consumer Pattern**

This is a classic concurrency design pattern. It decouples the process that creates work (the Producer) from the process that does the work (the Consumer) using a shared, thread-safe queue.

  * **In this project**:
      * **Producers**: Any part of the application that calls the `scheduler.submit(job)` method.
      * **Shared Queue**: The `jobQueue`.
      * **Consumers**: The `WorkerThread`s in the thread pool.
  * **Benefit**: The producers can add jobs very quickly without waiting for them to be completed, and the consumers can work independently at their own pace.

#### **Synchronization (Locks & Conditions)**

When multiple threads access a shared resource (like our `jobQueue`), they can interfere with each other, leading to data corruption. This is called a **race condition**. Synchronization primitives are the tools we use to control access and ensure thread safety.

  * **`ReentrantLock`**: This acts like a **key to a room**. Only one thread can hold the key (`lock()`) at a time, so only one thread can be inside the "room" (the critical section of code) modifying the queue.
  * **`Condition` Variable**: This is like a **waiting area inside the locked room**.
      * If a worker thread enters and finds no jobs, it goes to the waiting area (`newJobCondition.await()`), releasing the lock so others can enter.
      * When a producer adds a new job, it "pokes" one of the waiting threads (`newJobCondition.signal()`) to wake up, re-acquire the lock, and check for work again. This is far more efficient than busy-waiting (a thread constantly looping and asking, "Is there a job yet?").

-----

## Architectural Breakdown 🏗️

The project is built from three main classes that work together.

#### `Job.java` - The Task Blueprint

  * **Role**: A simple data class that represents a unit of work.
  * **Key Features**:
      * It holds a `name`, a `priority`, and the actual task logic as a `Runnable`.
      * It implements the `Comparable` interface. The `compareTo` method compares jobs based on their priority, which is essential for the `PriorityQueue` to function correctly.

#### `JobScheduler.java` - The Central Manager

  * **Role**: The "brain" of the operation. It manages the entire lifecycle of jobs and workers.
  * **Key Components**:
      * `PriorityQueue<Job> jobQueue`: The shared, thread-safe queue where jobs wait.
      * `WorkerThread[] workerThreads`: The thread pool.
      * `ReentrantLock queueLock` and `Condition newJobCondition`: The synchronization primitives used to control access to the queue.
      * `submit(Job job)`: The public method for producers to add new jobs.
      * `shutdown()`: The method to gracefully stop all worker threads.

#### `WorkerThread` (Inner Class) - The Employee

  * **Role**: To execute jobs. It's defined as an inner class within `JobScheduler` so it has direct and easy access to the shared resources (`jobQueue`, `queueLock`, etc.).
  * **Lifecycle**: Its `run()` method contains an infinite loop with the following logic:
    1.  Lock the queue.
    2.  Wait on the condition variable until a job is available.
    3.  When woken up, take the highest-priority job from the queue.
    4.  **Unlock the queue.**
    5.  **Execute the job.**
    6.  Loop back to step 1.

-----

## In-Depth Code Rationale 💡

Two specific pieces of the implementation are critical for performance and correctness.

#### **The "Guarded Block" and Spurious Wakeups**

Inside the `WorkerThread`'s `run` method, you see this pattern:

```java
while (jobQueue.isEmpty() && !isShutdown.get()) {
    newJobCondition.await();
}
```

This is called a **guarded block**. You might ask, "Why a `while` loop? Why not just an `if` statement?" This is to protect against a rare phenomenon called **spurious wakeups**, where a waiting thread can wake up without having been signaled. By re-checking the condition (`jobQueue.isEmpty()`) in a `while` loop, we guarantee the thread only proceeds if there is actually a job to do.

#### **Executing Outside the Lock**

Notice that the job is retrieved from the queue *inside* the lock, but `job.run()` is called *after* the lock is released:

```java
// ... inside run() method
Job job;
queueLock.lock();
try {
    // ... waiting logic ...
    job = jobQueue.poll();
} finally {
    queueLock.unlock(); // Lock is released here
}

if (job != null) {
    job.run(); // Job is executed here, outside the lock
}
```

This is a **major performance optimization**. If `job.run()` were called inside the lock, a single long-running job (e.g., one that takes 10 seconds) would hold the lock for its entire duration, preventing all other worker threads from even picking up new jobs. By executing outside the lock, we maximize concurrency and ensure the queue remains available.

---
## 🚀 How to Run

The project is built with standard Java and has no external dependencies.

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/GauravSharma018/Multi-threaded-Job-Scheduler
    cd Multi-threaded-Job-Scheduler
    ```

2.  **Compile the code**:
    Navigate to the `src` directory and compile all `.java` files.
    ```bash
    cd src
    javac com/scheduler/*.java
    ```

3.  **Run the application**:
    From the `src` directory, run the `Main` class.
    ```bash
    java com.scheduler.Main
    ```

---
### Example Output

The output will show jobs being submitted and then executed by the worker threads, with higher-priority jobs (like B and G) being picked first. Finally, it will display a benchmark comparing the multi-threaded performance to a single-threaded approach.


## 📄 License

This project is open-source and available under the **MIT License**. See the `LICENSE` file for more details.
