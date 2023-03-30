package lab2;

import static lab2.Constants.unit;
import static lab2.Printer.print;

public record Task(int id, long executionTime) implements Runnable {

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        try {
            print(threadName + " has started executing task: " + id);
            Thread.sleep(unit.toMillis(executionTime));
            print(threadName + " has finished task: " + id);
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(threadName + " was interrupted doing task: " + id, e);
        }
    }
}
