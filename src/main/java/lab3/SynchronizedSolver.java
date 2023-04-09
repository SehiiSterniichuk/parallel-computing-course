package lab3;

import java.util.LinkedList;
import java.util.List;

public class SynchronizedSolver {
    private int diff = 0;
    private int max = Integer.MIN_VALUE;
    private int waitingTimeForSync = 0;
    private final TimeCounter[] counters;
    private final List<Thread> threads;

    public SynchronizedSolver(int[] array, int threadNumber) {
        int length = array.length;
        if (length % threadNumber != 0) {
            throw new IllegalArgumentException(
                    String.format("length:%d %% threadNumber:%d != 0", length, threadNumber));
        }
        int step = length / threadNumber;
        counters = new TimeCounter[threadNumber];
        threads = new LinkedList<>();
        for (int i = 0; i < threadNumber; i++) {
            TimeCounter counter = new TimeCounter();
            counters[i] = counter;
            int finalI = i;
            threads.add(new Thread(() -> {
                int start = finalI * step;
                solveInRange(array, start, start + step, counter);
            }));
        }
    }

    public Result solve() throws InterruptedException {
        threads.forEach(Thread::start);
        for(var i : threads){
            i.join();
        }
        for(var c : counters){
            waitingTimeForSync += c.time;
        }
        return new Result(diff, max);
    }

    private void solveInRange(int[] array, int start, int end, TimeCounter counter) {
        for (int i = start; i < end; i++) {
            int value = array[i];
            if (value % 2 == 0) {
                counter.setStart();
                write(value, counter);
            }
        }
    }

    private synchronized void write(int value, TimeCounter counter) {
        counter.setFinish();
        diff -= value;
        if (max < value) {
            max = value;
        }
    }

    public long getWaitingTimeForSync() {
        return waitingTimeForSync;
    }

    private static class TimeCounter {
        long start;
        long time = 0;

        void setStart() {
            start = System.currentTimeMillis();
        }

        void setFinish() {
            long finish = System.currentTimeMillis();
            long res = finish - start;
            time += res;
        }
    }
}
