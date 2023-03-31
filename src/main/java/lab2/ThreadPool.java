package lab2;

import java.util.*;

import static lab2.Printer.print;

public class ThreadPool {

    private final Queue<Task> queue;

    private final List<Thread> workers;

    private final Object lock = new Object();

    private boolean terminated = false;//todo should it be volatile?

    private int counterOfWorkingThreads = 0;

    private boolean initialized = false;

    private final Object producerWaiter = new Object();

    public ThreadPool(int numberOfWorkers) {
        if (numberOfWorkers < 1) {
            throw new IllegalArgumentException("number of workers must be bigger than 0");
        }
        queue = new LinkedList<>();
        workers = new ArrayList<>(numberOfWorkers);
        for (int i = 0; i < numberOfWorkers; ++i) {
            workers.add(new Thread(this::routine));
        }
    }

    public void addTask(Task task) {
        if (task == null) return;
        synchronized (lock) {
            if (terminated || counterOfWorkingThreads >= 1) {//counterOfWorkingThreads >= 1 means that at least 1 worker is working on the task
                print("The thread pool rejected the task: " + task);
                return;
            }
            queue.add(task);
        }
        print("Producer has added task: " + task);
    }

    public void execute() {
        synchronized (lock) {
            if (initialized) {
                lock.notifyAll();
                print("The producer notified the workers to start performing new tasks");
                return;
            } else if (terminated) {
                return;
            }
            counterOfWorkingThreads = workers.size();
            initialized = true;
        }
        print("The threadPool has started threads");
        workers.forEach(Thread::start);
    }

    private void routine() {
        final String threadName = Thread.currentThread().getName();
        while (true) {
            Task task;
            synchronized (lock) {
                try {
                    while (!terminated && queue.isEmpty()) {
                        waitForANewTask(threadName);
                    }
                    if (terminated && queue.isEmpty()) {
                        print(threadName + " is terminated");
                        return;
                    }
                    task = queue.poll();
                } catch (InterruptedException e) {
                    print(threadName + " was interrupted while waiting for a new task");
                    return;
                }
            }
            try {
                task.run();
            } catch (RuntimeInterruptedException e) {
                print(e.getMessage());
                return;
            }
        }
    }

    private void waitForANewTask(String threadName) throws InterruptedException {
        counterOfWorkingThreads--;
        if (counterOfWorkingThreads < 1) notifyProducer(threadName);
        print(threadName + " is waiting for a new task👉👈");
        lock.wait();
        counterOfWorkingThreads++;
        print(threadName + " has woken up");
    }

    private void notifyProducer(String threadName) {
        synchronized (producerWaiter) {
            producerWaiter.notify();
            print(threadName + " has notified the Producer to fill the queue");
        }
    }

    public void terminate() {
        print("The termination of thread pool has started");
        synchronized (lock) {
            if (terminated) {
                print("The termination of the thread pool has already been completed.");
                return;
            }
            terminated = true;
            lock.notifyAll();//*signal when termination*
        }
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        print("The termination of thread pool has finished");
    }

    public void terminateImmediately() {
        print("The producer has commanded the termination of the thread pool.☠️");
        terminated = true;
        for (Thread thread : workers) {
            thread.interrupt();
        }
    }

    public boolean producerCanAddNewTasks() {
        synchronized (lock) {
            print("queue.size(): " + queue.size() +
                    " counterOfWorkingThreads: " + counterOfWorkingThreads);
            return initialized && !terminated && (queue.size() < 1) && (counterOfWorkingThreads < 1);
        }
    }

    public Object getProducerWaiter() {
        return producerWaiter;
    }
}
