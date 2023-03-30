package lab2;

import java.util.Random;

import static lab2.Constants.*;

public class Producer {

    public boolean isWorking = false;

    public int nextId = 0;

    private final Random rand = new Random();

    private long totalTimeOfExecutionAllTasks = 0L;

    private final ThreadPool pool;

    public Producer(ThreadPool pool) {
        this.pool = pool;
    }


    public static void main(String[] args) throws InterruptedException {
        System.out.println("Program has started");
        ThreadPool threadPool = new ThreadPool(NUMBER_OF_THREADS);
        Producer producer = new Producer(threadPool);
        final Object waiter = threadPool.getProducerWaiter();
        final int NUMBER_OF_LIFECYCLES = 8;
        for (int i = 0; i < NUMBER_OF_LIFECYCLES; i++) {
            Thread thread = new Thread(producer::work);
            thread.start();
            sleep(Constants.unit.toMillis(QUEUE_FILLING_TIME));
            producer.isWorking = false;
            thread.join();
            System.out.println("The queue has finished to fill");
            threadPool.execute();
            while (threadPool.hasTasksToDo()) {
                synchronized (waiter){
                    waiter.wait();
                }
            }
        }
        threadPool.terminate();
    }

    private static void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void work() {
        System.out.println("The queue has started to fill");
        isWorking = true;
        while (isWorking) {
            Task newTask = generateTask();
            boolean isAcceptableTask = checkTask(newTask);
            if (!isAcceptableTask) continue;
            pool.addTask(newTask);
            totalTimeOfExecutionAllTasks += newTask.executionTime();
            sleep(unit.toMillis(QUEUE_FILLING_TIME) / PRODUCER_CREATE_COEFFICIENT);
        }
        totalTimeOfExecutionAllTasks = 0;
        nextId = 0;
    }

    private boolean checkTask(Task newTask) {
        return totalTimeOfExecutionAllTasks + newTask.executionTime() <= MAXIMUM_TIME_OF_EXECUTION_ALL_TASKS;
    }

    private Task generateTask() {
        long randomExecutionTime = rand.nextLong(MAX_TASK_DURATION - MIN_TASK_DURATION + 1) + MIN_TASK_DURATION;
        return new Task(getNextId(), randomExecutionTime);
    }

    private int getNextId() {
        if (nextId + 1 == Integer.MAX_VALUE) {
            nextId = 0;
        }
        return nextId++;
    }
}
