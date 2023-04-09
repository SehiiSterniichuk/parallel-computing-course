package lab3;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicSolver {
    private final AtomicInteger diff = new AtomicInteger();
    private final AtomicInteger max = new AtomicInteger(Integer.MIN_VALUE);
    private final List<Thread> threads;

    public AtomicSolver(int[] array, int threadNumber) {
        int length = array.length;
        if (length % threadNumber != 0) {
            throw new IllegalArgumentException(
                    String.format("length:%d %% threadNumber:%d != 0", length, threadNumber));
        }
        int step = length / threadNumber;
        threads = new LinkedList<>();
        for (int i = 0; i < threadNumber; i++) {
            int finalI = i;
            threads.add(new Thread(() -> {
                int start = finalI * step;
                solveInRange(array, start, start + step);
            }));
        }
    }

    public Result solve() throws InterruptedException {
        threads.forEach(Thread::start);
        for (var i : threads) {
            i.join();
        }
        return new Result(diff.get(), max.get());
    }

    private void solveInRange(int[] array, int start, int end) {
        int localMax = max.get();
        for (int i = start; i < end; i++) {
            int value = array[i];
            if (value % 2 == 0) {
                diff.getAndAdd(-value);
                if (localMax < value) {
                    localMax = max.updateAndGet(currentMax -> Math.max(currentMax, value));
                }
            }
        }
    }
}
