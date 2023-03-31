package lab2;

import java.util.Random;
import java.lang.*;

import static lab2.Constants.*;
import static lab2.Printer.print;

public class Producer {

    public boolean isWorking = false;

    public int nextId = 0;

    private static final Random rand = new Random();

    private long totalTimeOfExecutionAllTasks = 0L;

    private final ThreadPool pool;

    public Producer(ThreadPool pool) {
        this.pool = pool;
    }


    public static void main(String[] args) throws InterruptedException {
        //todo write statistic counter
        System.out.println("Program has started");
        ThreadPool threadPool = new ThreadPool(NUMBER_OF_THREADS);
        Producer producer = new Producer(threadPool);
        final Object waiter = threadPool.getProducerWaiter();
        final int NUMBER_OF_LIFECYCLES = 4;
        final int INTERRUPT_CYCLE_ID = 2;
        for (int i = 0; i < NUMBER_OF_LIFECYCLES; i++) {
            Thread thread = new Thread(producer::work);
            thread.start();
            sleep(Constants.unit.toMillis(QUEUE_FILLING_TIME));
            producer.isWorking = false;
            thread.join();
            System.out.println("The queue has finished to fill");
            threadPool.execute();
            if(INTERRUPT_CYCLE_ID == i){
                sleep(unit.toMillis(getRandomExecutionTime()));
                threadPool.terminateImmediately();
                break;
            }
            while (!threadPool.producerCanAddNewTasks()) {
                synchronized (waiter) {
                    waiter.wait();
                }
            }
            print("Producer can add new tasks");
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
        System.out.println("The queue has started to fill");//todo check sout
        isWorking = true;
        while (isWorking) {
            Task newTask = generateTask();
            boolean isAcceptableTask = checkTask(newTask);
            if (!isAcceptableTask) continue;
            pool.addTask(newTask);
            totalTimeOfExecutionAllTasks += newTask.executionTime();
            sleep(PRODUCER_WORK_TIME);
        }
        totalTimeOfExecutionAllTasks = 0;
        nextId = 0;
    }

    private boolean checkTask(Task newTask) {
        return totalTimeOfExecutionAllTasks + newTask.executionTime() <= MAXIMUM_TIME_OF_EXECUTION_ALL_TASKS;
    }

    private Task generateTask() {
        long randomExecutionTime = getRandomExecutionTime();
        return new Task(getNextId(), randomExecutionTime);
    }

    private static long getRandomExecutionTime() {
        return rand.nextLong(MAX_TASK_DURATION - MIN_TASK_DURATION + 1) + MIN_TASK_DURATION;
    }

    private int getNextId() {
        if (nextId + 1 == Integer.MAX_VALUE) {
            nextId = 0;
        }
        return nextId++;
    }
}
