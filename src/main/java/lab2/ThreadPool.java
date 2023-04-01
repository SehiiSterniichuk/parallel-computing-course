package lab2;

import java.util.*;

import static lab2.Printer.print;

public class ThreadPool {

    private final TaskQueue queue;

    private final QueueTimeCounter queueCounter;

    private int executionNumber = 0;

    private final List<Thread> workers;

    private final ThreadWaitingCounter waitingCounter;

    private final Object lock;

    private boolean terminated = false;

    private int counterOfWorkingThreads = 0;

    private boolean initialized = false;

    private final Object producerWaiter;

    public ThreadPool(int numberOfWorkers) {
        if (numberOfWorkers < 1) {
            throw new IllegalArgumentException("number of workers must be bigger than 0");
        }
        workers = new ArrayList<>(numberOfWorkers);
        for (int i = 0; i < numberOfWorkers; ++i) {
            workers.add(new Thread(this::routine));
        }
        producerWaiter = new Object();
        lock = new Object();
        queue = new TaskQueue();
        queueCounter = new QueueTimeCounter();
        waitingCounter = new ThreadWaitingCounter();
    }

    public void addTask(Task task) {
        if (task == null) return;
        boolean acceptedTask;
        synchronized (lock) {
            if (terminated || counterOfWorkingThreads >= 1) {//counterOfWorkingThreads >= 1 means that at least 1 worker is working on the task
                print("The thread pool rejected the task: " + task);
                return;
            }
            acceptedTask = queue.add(task);
        }
        if (acceptedTask) print("Producer has added task: " + task);
    }

    public void execute() {
        synchronized (lock) {
            if (terminated) {
                return;
            }
            queueCounter.setStart();
            executionNumber++;
            if (initialized) {
                lock.notifyAll();
                print("The producer notified the workers to start performing new tasks");
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
                    if (!terminated && queue.isEmpty()) {
                        if (counterOfWorkingThreads == workers.size()) queueCounter.setFinish();
                        long start = System.nanoTime();
                        waitForANewTask(threadName);
                        waitingCounter.count(start);
                    }
                    if (terminated) {
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
        while (!terminated && queue.isEmpty()) {
            lock.wait();
        }
        counterOfWorkingThreads++;
        print(threadName + " has woken up");
    }

    private void notifyProducer(String threadName) {
        synchronized (producerWaiter) {
            producerWaiter.notify();
        }
        print(threadName + " has notified the Producer to fill the queue");
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
        print("queue.size(): " + queue.size());
    }

    public void interrupt() {
        print("The producer has commanded the interruption of the thread pool.☠️");
        synchronized (lock) {
            terminated = true;
        }
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

    public Result getResult() {
        int numberOfRejectedTasks = queue.getNumberOfRejectedTasks();
        float averageNumberOfRejectedTasks = ((float) numberOfRejectedTasks) / executionNumber;
        return new Result(waitingCounter.averageTimeOfThreadInWaitingState(),
                queueCounter.maxTimeOfActiveQueue,
                queueCounter.minTimeOfActiveQueue,
                numberOfRejectedTasks,
                averageNumberOfRejectedTasks);
    }

    private static class QueueTimeCounter {

        private long maxTimeOfActiveQueue = 0;

        private long minTimeOfActiveQueue = 0;

        private long start;

        private void setStart() {
            start = System.nanoTime();
        }

        private void setFinish() {
            long finish = System.nanoTime();
            long result = finish - start;
            if (minTimeOfActiveQueue == 0) {
                minTimeOfActiveQueue = maxTimeOfActiveQueue = result;
            } else if (maxTimeOfActiveQueue < result) {
                maxTimeOfActiveQueue = result;
            } else if (minTimeOfActiveQueue > result) {
                minTimeOfActiveQueue = result;
            }
        }
    }

    private static class ThreadWaitingCounter {

        private long totalWaitingTime = 0;

        private int numberOfWaitingThreads = 0;

        private void count(long start) {
            long finish = System.nanoTime();
            long result = finish - start;
            totalWaitingTime += result;
            numberOfWaitingThreads++;
        }

        private long averageTimeOfThreadInWaitingState() {
            return totalWaitingTime / numberOfWaitingThreads;
        }
    }
}
