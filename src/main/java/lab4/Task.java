package lab4;

import lab1.Matrix;

public class Task implements Runnable {
    private final Matrix matrix;
    private final long id;

    private volatile Status status = Status.WAITING;

    private long timeOfExecution;

    private final int numberOfThreads;

    public Task(Matrix matrix, long id, int numberOfThreads) {
        this.matrix = matrix;
        this.id = id;
        this.numberOfThreads = numberOfThreads;
    }

    @Override
    public void run() {
        status = Status.RUNNING;
        timeOfExecution = lab1.Main.solve(matrix, numberOfThreads);
        status = Status.DONE;
    }

    public long getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public long getTimeOfExecution() {
        return timeOfExecution;
    }
}
