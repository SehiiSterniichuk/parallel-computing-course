package lab4;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;

public class Server {
    private final ServerSocket serverSocket;
    private volatile boolean isWorking = true;

    private final ConcurrentHashMap<Long, Task> map;

    private final ExecutorService executor;

    static final long TIME_TO_WORK = TimeUnit.SECONDS.toMillis(60);
    static final int TIMEOUT = (int) (TIME_TO_WORK);

    public Server(ServerSocket serverSocket) throws SocketException {
        this.serverSocket = serverSocket;
        serverSocket.setSoTimeout(TIMEOUT);
        map = new ConcurrentHashMap<>();
        executor = Executors.newSingleThreadExecutor();
    }

    public void start() throws InterruptedException {
        Thread workThread = new Thread(this::work);
        workThread.start();
        Thread.sleep(TIME_TO_WORK);
        isWorking = false;
        executor.shutdown();
        workThread.join();
    }

    private void work() {
        try (var clientHandlerExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3)) {
            while (isWorking || !clientHandlerExecutor.getQueue().isEmpty() ||
                    clientHandlerExecutor.getActiveCount() > 0 || !map.isEmpty()) {
                ClientHandler handler = new ClientHandler(serverSocket.accept(), executor, map);
                clientHandlerExecutor.submit(handler);
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout");
        } catch (IOException e) {
            isWorking = false;
            throw new RuntimeException(e);
        }
    }
}
