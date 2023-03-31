package lab2;

import java.util.concurrent.TimeUnit;

public interface Constants {
    TimeUnit unit = TimeUnit.MILLISECONDS;
    long QUEUE_FILLING_TIME = 40;
    long PRODUCER_WORK_TIME = unit.toMillis(QUEUE_FILLING_TIME) / 50;
    int NUMBER_OF_THREADS = 4;
    long MAXIMUM_TIME_OF_EXECUTION_ALL_TASKS = 60;
    long MIN_TASK_DURATION = 6;
    long MAX_TASK_DURATION = 16;
}
