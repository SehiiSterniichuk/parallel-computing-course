package lab2;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static lab2.Printer.print;

public class ThreadPool {
    private final Queue<Task> queue;
    private final Condition taskWaiter;
    private final List<Thread> workers;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    private boolean terminated = false;

    private int counterOfWorkingThreads = 0;

    private boolean initialized = false;


    private final Object producerWaiter = new Object();

    public ThreadPool(int numberOfWorkers) {
        if (numberOfWorkers < 1) {
            throw new IllegalArgumentException("number of workers must be bigger than 0");
        }
        queue = new LinkedList<>();
        var readWriteLock = new ReentrantReadWriteLock();
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
        taskWaiter = writeLock.newCondition();
        workers = new ArrayList<>(numberOfWorkers);
        for (int i = 0; i < numberOfWorkers; ++i) {
            workers.add(new Thread(this::routine));
        }
    }

    public void addTask(Task task) {
        if (task == null) return;
        readLock.lock();
        try {
            if (terminated || counterOfWorkingThreads >= 1) {//counterOfWorkingThreads >= 1 means that at least 1 worker is working on the task
                return;
            }
        } finally {
            readLock.unlock();
        }
        writeLock.lock();
        queue.add(task);
        writeLock.unlock();
        print("Producer has added task: " + task);
    }

    public void terminate() {
        print("Termination of thread pool has started");
        writeLock.lock();
        try {
            if (terminated) {
                return;
            }
            terminated = true;
            taskWaiter.signalAll();//*signal when termination*
        } finally {
            writeLock.unlock();
        }
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        print("Termination of thread pool has finished");
    }

    public void execute() {
        writeLock.lock();
        try {
            if (initialized) {
                taskWaiter.signalAll();
                return;
            } else if (terminated) {
                return;
            }
        } finally {
            writeLock.unlock();
        }
        counterOfWorkingThreads = workers.size();
        initialized = true;
        print("ThreadPool has started execution");
        workers.forEach(Thread::start);
    }

    private void routine() {
        final String threadName = Thread.currentThread().getName();
        while (true) {
            Task task;
            writeLock.lock();
            try {
                while (!terminated && queue.isEmpty()) {
                    notifyProducer();
                    counterOfWorkingThreads--;
                    print(threadName + " is waiting for a new task👉👈");
                    taskWaiter.await();
                    print(threadName + " has woken up");
                    counterOfWorkingThreads++;
                }
                if (terminated && queue.isEmpty()) {
                    return;
                }
                task = queue.poll();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                writeLock.unlock();
            }
            task.run();
        }
    }

    private void notifyProducer() {
        synchronized (producerWaiter) {
            producerWaiter.notify();
        }
    }

    public boolean hasTasksToDo() {
        readLock.lock();
        try {
            return queue.size() > 0;
        } finally {
            readLock.unlock();
        }
    }

    public Object getProducerWaiter() {
        return producerWaiter;
    }
}
