package lab2;

import static lab2.Constants.unit;

public record Task(int id, long executionTime) {

    public void run() throws InterruptedException {
        String threadName = Thread.currentThread().getName();
        System.out.println(threadName + " has started executing task: " + id);
        Thread.sleep(unit.toMillis(executionTime));
        System.out.println(threadName + " has finished task: " + id);
    }
}
