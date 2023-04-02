package lab2;

import static lab2.Constants.unit;
import static lab2.Printer.print;

public record Task(int id, long executionTime) {

    public void run() throws InterruptedException {
        String threadName = Thread.currentThread().getName();
        print(threadName + " has started executing task: " + id);
        Thread.sleep(unit.toMillis(executionTime));
        print(threadName + " has finished task: " + id);
    }
}
