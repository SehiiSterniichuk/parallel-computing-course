package lab2;

import java.util.concurrent.TimeUnit;

import static lab2.Constants.unit;

public record Result(long averageTimeOfThreadInWaitingState,
                     long maxTimeOfActiveQueue,
                     long minTimeOfActiveQueue,
                     int numberOfRejectedTasks,
                     float averageNumberOfRejectedTasks) {

    @Override
    public String toString() {
        return "Result{" +
                "\naverageTimeOfThreadInWaitingState=" + convertNanoToUnit(averageTimeOfThreadInWaitingState) +
                ",\nmaxTimeOfActiveQueue=" + convertNanoToUnit(maxTimeOfActiveQueue) +
                ",\nminTimeOfActiveQueue=" + convertNanoToUnit(minTimeOfActiveQueue) +
                ",\nnumberOfRejectedTasks=" + numberOfRejectedTasks +
                ",\naverageNumberOfRejectedTasks=" + averageNumberOfRejectedTasks +
                "\n}";
    }

    private String convertNanoToUnit(long time){
        long convertedTime = unit.convert(time, TimeUnit.NANOSECONDS);
        return convertedTime + " " + unit.toString().toLowerCase();
    }
}
