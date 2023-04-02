package lab2;

import java.util.ArrayList;
import java.util.List;

import static lab2.Printer.print;

public class ThreadPool {

    private final TaskQueue queue;

    private final QueueTimeCounter queueCounter;//вимірює мінімальний та максимальний час заповненої черги

    private int executionNumber = 0;//лічильник кількості інтервалів заповнення черги

    private final List<Thread> workers;

    private final ThreadWaitingCounter waitingCounter;//лічильник середнього часу очікування потоку

    private final Object lock;//Об'єкт синхронізації. Монітор

    private boolean isTerminated = false;//прапор що позначає чи пул потоків не завершений(термінований) ще

    private int counterOfWorkingThreads = 0;
    //лічильник не сплячих потоків який потрібний для того, щоб Виробник додавав нові задачі тільки коли всі потоки завершили свої задачі та сплять

    private boolean initialized = false;//прапор позначає що пул запустив свої потоки

    private final Object producerWaiter;
    //монітор потрібен для синхронізації роботи з Виробником задач. Пул "будить" інший потік повідомляючи що вже пора додавати нові завдання

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
        synchronized (lock) {
            if (isTerminated || counterOfWorkingThreads >= 1) {
                //counterOfWorkingThreads >= 1 означає що лишився хоча б 1 потік який ще працює над своєю задачею
                print("The thread pool rejected the task: " + task);
                return;
            }
            if (!queue.add(task)) return;//якщо черга вертає false отже задача не поміщається в ліміт
        }
        print("Producer has added task: " + task);//вивід на екран успішно доданих задач
    }

    public void execute() {
        synchronized (lock) {
            if (isTerminated) {
                return;
            }
            queueCounter.setStart();//починаємо відлік часу заповненої черги
            executionNumber++;
            if (initialized) {
                lock.notifyAll();//якщо initialized == true тоді потоки вже працюють і їх треба всього лише розбудити, щоб вони почали працювати
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
                    if (!isTerminated && queue.isEmpty()) {
                        if (counterOfWorkingThreads == workers.size()) queueCounter.setFinish();
    //якщо counterOfWorkingThreads == workers.size(), то це перший потік який виявив що черга пуста, отже можна зупиняти queueCounter лічильник
                        long start = System.nanoTime();
                        waitForANewTask(threadName);
                        if (!isTerminated) {
    /*потік може проснутися через те що робота пула завершена і
    цей результат може бути меншим за нормальний час очікування потоку нових задач
    тому потрібна умова !isTerminated*/
                            waitingCounter.count(start);
                        }
                    }
                    if (isTerminated) {
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
                //Роботу пулу треба моментально зупинити без очікування на завершення поточних завдань. Тому було отримано RuntimeInterruptedException
                print(e.getMessage());
                return;
            }
        }
    }

    private void waitForANewTask(String threadName) throws InterruptedException {
        counterOfWorkingThreads--;
        if (counterOfWorkingThreads < 1) notifyProducer(threadName);
        // якщо counterOfWorkingThreads < 1 тоді це останній робочий потік, отже потрібно починати заповнення черги
        print(threadName + " is waiting for a new task👉👈");
        while (!isTerminated && queue.isEmpty()) {
            lock.wait();//чекаємо поки робота пулу не завершиться або не додадуть нових задач у чергу
            //Одночасно з тим що ми починаємо очікування, то звільняється монітор lock який потрібно отримати для виробника,
            // щоб перевірити чи можна додавати нові задачі. (Про метод producerCanAddNewTasks())
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

    public void terminate() {//безпечна зупинка з очікуванням завершення активних задач
        print("The termination of thread pool has started");
        synchronized (lock) {
            if (isTerminated) {
                print("The termination of the thread pool has already been completed.");
                return;
            }
            isTerminated = true;
            lock.notifyAll();//будимо всі потоки що очікували
        }
        for (Thread worker : workers) {
            try {
                worker.join();//очікуємо завершення потоків
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        print("The termination of thread pool has finished");
        print("queue.size(): " + queue.size());
    }

    public void interrupt() {//моментальна зупинка без очікування завершення активних задач
        print("The producer has commanded the interruption of the thread pool.☠️");
        synchronized (lock) {
            isTerminated = true;
        }
        for (Thread thread : workers) {
            thread.interrupt();
        }
    }

    public boolean producerCanAddNewTasks() {
        synchronized (lock) {
            return initialized && !isTerminated && (queue.size() < 1) && (counterOfWorkingThreads < 1);
        }
    }

    public Object getProducerWaiter() {
        return producerWaiter;
    }

    public Result getResult() {
        int numberOfRejectedTasks = queue.getNumberOfRejectedTasks();
        float averageNumberOfRejectedTasks = ((float) numberOfRejectedTasks) / executionNumber;
        int numberOfAcceptedTasks = queue.getNumberOfAcceptedTasks();
        float averageNumberOfAcceptedTasks = ((float) numberOfAcceptedTasks) / executionNumber;
        return new Result(waitingCounter.averageTimeOfThreadInWaitingState(),
                queueCounter.maxTimeOfActiveQueue,
                queueCounter.minTimeOfActiveQueue,
                numberOfRejectedTasks,
                averageNumberOfRejectedTasks,
                numberOfAcceptedTasks,
                averageNumberOfAcceptedTasks
        );
    }

    private static class QueueTimeCounter {

        private long maxTimeOfActiveQueue = -1;

        private long minTimeOfActiveQueue = -1;

        private long start;

        private void setStart() {
            start = System.nanoTime();
        }

        private void setFinish() {
            long finish = System.nanoTime();
            long result = finish - start;
            if (minTimeOfActiveQueue < 0) {
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
