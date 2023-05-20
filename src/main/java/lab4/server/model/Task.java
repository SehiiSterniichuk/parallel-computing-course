package lab4.server.model;

import lab1.Matrix;
import lab4.config.Status;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Task {
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

    public Result run() {
        status = Status.RUNNING;
        timeOfExecution = lab1.Main.solve(matrix, numberOfThreads);
        status = Status.DONE;
        return createResult();
    }

    public Status getStatus() {
        return status;
    }

    public Result getResult() throws ExecutionException, InterruptedException {
        checkFutureNull();
        return future.get();
    }

    public Result getResult(long time, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        checkFutureNull();
        return future.get(time, unit);
    }

    private Result createResult() {
        return new Result(matrix.data, timeOfExecution);
    }

    public record Result(double[][] data, long timeOfExecution) {
    }

    private void checkFutureNull() {
        if (future == null) {
            throw new IllegalStateException("The future object has not yet been assigned");
        }
    }

    private Future<Task.Result> future = null;

    public void setFuture(Future<Task.Result> future) {
        this.future = future;
    }


    public int size() {
        return matrix.size;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", size=" + matrix.size +
                ", numberOfThreads=" + numberOfThreads +
                '}';
    }
}
