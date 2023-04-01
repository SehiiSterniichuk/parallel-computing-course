package lab2;

import java.util.Random;

import static lab2.Constants.*;
import static lab2.Printer.print;

public class Producer {

    private volatile boolean isWorking = false;

    private int nextId = 0;

    private final ThreadPool pool;

    private static final Random rand = new Random();

    public Producer(ThreadPool pool) {
        this.pool = pool;
    }


    public void work() {
        print("The queue has started to fill");
        isWorking = true;
        while (isWorking) {
            Task newTask = generateTask();
            Main.sleep(PRODUCER_WORK_TIME);
            pool.addTask(newTask);
        }
        nextId = 0;
    }

    private Task generateTask() {
        long randomExecutionTime = getRandomExecutionTime();
        return new Task(getNextId(), randomExecutionTime);
    }

    static long getRandomExecutionTime() {
        return rand.nextLong(MAX_TASK_DURATION - MIN_TASK_DURATION + 1) + MIN_TASK_DURATION;
    }

    private int getNextId() {
        if (nextId + 1 == Integer.MAX_VALUE) {
            nextId = 0;
        }
        return nextId++;
    }

    public void stop() {
        isWorking = false;
    }
}
