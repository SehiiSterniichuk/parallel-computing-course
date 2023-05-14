package lab4.client;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientRunner {
    private static final String host = "localhost";
    private static final int port = 1234;
    private static final int CPU_CORES = 8;
    private static final int CPU_LOGICAL_CORES = 16;
    private static final int MIN_THREADS = CPU_CORES / 2;
    private static final List<Integer> threadNumbers = List.of(
            1,
            MIN_THREADS,
            CPU_CORES,
            CPU_LOGICAL_CORES,
            CPU_LOGICAL_CORES * 2,
            CPU_LOGICAL_CORES * 4,
            CPU_LOGICAL_CORES * 8,
            CPU_LOGICAL_CORES * 16
    );
    private static final List<Integer> dimensionNumbers = List.of(
            MIN_THREADS * 256 * 16,
            MIN_THREADS * 256 / 2,
            MIN_THREADS * 256 * 2,
            MIN_THREADS * 256 * 8,
            MIN_THREADS * 256 * 4,
            MIN_THREADS * 256
    );

    public static void main(String[] args) {
        try (ExecutorService executor = Executors.newFixedThreadPool(3)) {
            threadNumbers.forEach(thread -> dimensionNumbers.forEach(size -> {
                Client client = new Client(host, port, size, thread);
                executor.submit(client);
            }));
            executor.shutdown();//wait all clients
        }
        Client killer = new Client(host, port, -1, -1);
        killer.run();
        System.out.println("Clients have finished work");
    }
}