package lab3;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final Random rand = new Random();
    public static final TimeUnit unit = TimeUnit.MILLISECONDS;

    private static final int THREAD_NUMBER = 8;

    private static final List<Integer> sizes = List.of(
            1000 * THREAD_NUMBER,
            5000 * THREAD_NUMBER,
            10_000 * THREAD_NUMBER,
            100_000 * THREAD_NUMBER,
            100_000_000 * THREAD_NUMBER
    );

    public static void main(String[] args) throws InterruptedException {
        for (var size : sizes) {
            System.out.println("========= Array length: " + size + " ================");
            int[] array = randomArray(size);
            TimeCounter counter = new TimeCounter();
            counter.setStart();
            Result result = singleThreadSolution(array);
            long time = counter.setFinish();
            String output = simpleThreadOutput(1, time, result);
            System.out.println(output);

            var synchronizedSolver = new SynchronizedSolver(array, THREAD_NUMBER);
            counter.setStart();
            result = synchronizedSolver.solve();
            time = counter.setFinish();
            output = synchronizedSolutionOutput(time, result, synchronizedSolver.getWaitingTimeForSync());
            System.out.println(output);

            var atomicSolver = new AtomicSolver(array, THREAD_NUMBER);
            counter.setStart();
            result = atomicSolver.solve();
            time = counter.setFinish();
            output = simpleThreadOutput(THREAD_NUMBER, time, result);
            System.out.println("Atomic solution\n" + output);
        }
    }

    private static String synchronizedSolutionOutput(long time,
                                                     Result result, long waitingTimeOfSync) {
        String lowerCase = unit.toString().toLowerCase();
        return "Synchronized solution\nNumber of threads: " + Main.THREAD_NUMBER + '\n' +
                "Execution time: " + time + ' ' + lowerCase + '\n' +
                "Waiting time: " + waitingTimeOfSync + ' ' + lowerCase + '\n' +
                result + '\n';
    }

    private static String simpleThreadOutput(int threadNumber, long time, Result result) {
        return "Number of threads: " + threadNumber + '\n' +
                "Execution time: " + time + ' ' +
                unit.toString().toLowerCase() + '\n' +
                result + '\n';
    }

    private static Result singleThreadSolution(int[] array) {
        int diff = 0;
        int max = Integer.MIN_VALUE;
        for (int i : array) {
            if (i % 2 == 0) {
                diff -= i;
                if (max < i) {
                    max = i;
                }
            }
        }
        return new Result(diff, max);
    }

    private static int[] randomArray(int size) {
        int[] ints = new int[size];
        for (int i = 0; i < size; i++) {
            ints[i] = rand.nextInt() % 10_000;
        }
        return ints;
    }

    private static class TimeCounter {
        long start;

        void setStart() {
            start = System.nanoTime();
        }

        long setFinish() {
            long finish = System.nanoTime();
            long res = finish - start;
            return unit.convert(res, TimeUnit.NANOSECONDS);
        }
    }
}
