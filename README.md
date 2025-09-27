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



---
## 🚀 How to Run

The project is built with standard Java and has no external dependencies.

1.  **Clone the repository**:
    ```bash
    git clone [https://github.com/your-username/java-job-scheduler.git](https://github.com/your-username/java-job-scheduler.git)
    cd java-job-scheduler
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
