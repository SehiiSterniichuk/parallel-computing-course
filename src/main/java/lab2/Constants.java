package lab2;

import java.util.concurrent.TimeUnit;

public interface Constants {
    TimeUnit unit = TimeUnit.MILLISECONDS;
    long QUEUE_FILLING_TIME = 40;
    long PRODUCER_CREATE_COEFFICIENT = 1000;
    int NUMBER_OF_THREADS = 4;
    long MAXIMUM_TIME_OF_EXECUTION_ALL_TASKS = 60;
    long MIN_TASK_DURATION = 6;
    long MAX_TASK_DURATION = 14;
}
