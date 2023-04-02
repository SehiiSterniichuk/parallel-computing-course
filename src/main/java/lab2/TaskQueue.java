package lab2;

import java.util.LinkedList;
import java.util.Queue;

import static lab2.Constants.MAXIMUM_TIME_OF_EXECUTION_ALL_TASKS;

public class TaskQueue {

    private long totalTimeOfExecutionAllTasks = 0L;

    private int numberOfRejectedTasks = 0;
    private int numberOfAcceptedTasks = 0;

    private final Queue<Task> queue = new LinkedList<>();

    public boolean add(Task task) {
        if (checkTask(task)) {
            queue.add(task);
            totalTimeOfExecutionAllTasks += task.executionTime();
            numberOfAcceptedTasks++;
            return true;
        }
        numberOfRejectedTasks++;
        return false;
    }

    public Task poll() {
        Task poll = queue.poll();
        assert poll != null;
        totalTimeOfExecutionAllTasks -= poll.executionTime();
        return poll;
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    private boolean checkTask(Task newTask) {
        return totalTimeOfExecutionAllTasks + newTask.executionTime() <= MAXIMUM_TIME_OF_EXECUTION_ALL_TASKS;
    }

    public int getNumberOfRejectedTasks() {
        return numberOfRejectedTasks;
    }

    public int getNumberOfAcceptedTasks() {
        return numberOfAcceptedTasks;
    }
}