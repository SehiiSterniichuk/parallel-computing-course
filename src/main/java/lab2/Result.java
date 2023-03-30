package lab2;

public record Result(double averageTimeWaitingOfThread,
                     double averageTimeOfExecutingTask,
                     double maxTimeOfActiveQueue,
                     double minTimeOfActiveQueue) {
}
