package lab4;

import lab4.header.parameters.HeaderParametersHolder;
import lab4.header.parameters.NewTaskParameter;
import lab4.header.parameters.TaskId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static lab4.HeaderReader.readHeader;

public class Server {
    private final ServerSocket serverSocket;
    private volatile boolean isWorking = true;

    private final long timeToWork = TimeUnit.MINUTES.toMillis(1);

    private static final String methods = Arrays.stream(RequestType.values())
            .map(RequestType::name)
            .collect(Collectors.joining(", "));

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void start() throws IOException, InterruptedException {
        Thread workThread = new Thread(() -> {
            try {
                work();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        workThread.start();
        Thread.sleep(timeToWork);
        isWorking = false;
        workThread.join();
    }

    private void work() throws IOException {
        while (isWorking) {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                System.out.println("Before");
                Header header = readHeader(in);
                System.out.println("after");
                switch (header.type()) {
                    case POST_NEW_TASK -> addNewTask(in, out, header.parameters());
                    case GET_RESULT -> getTaskResult(in, out, header.parameters());
                    case GET_TASK_STATUS -> getTaskStatus(out, header.parameters());
                    case BAD_REQUEST -> out.println("Bad request. Available methods: " + methods);
                }
            }
        }
    }

    private void getTaskStatus(PrintWriter out, HeaderParametersHolder parameters) {
        if (parameters instanceof TaskId(var id)) {
            out.println("getTaskStatus is working and received id: " + id);
            return;
        }
        out.println("getTaskStatus smt went wrong...");
    }

    private void getTaskResult(BufferedReader in, PrintWriter out, HeaderParametersHolder parameters) {
        if (parameters instanceof TaskId(var id)) {
            out.println("getTaskResult is working and received id: " + id);
            return;
        }
        out.println("getTaskResult smt went wrong...");
    }

    private void addNewTask(BufferedReader in, PrintWriter out, HeaderParametersHolder parameters) {
        if (parameters instanceof NewTaskParameter(int size, int numberOfThreads)) {
            out.println("addNewTask is working and received id: " + size + " " + numberOfThreads);
            return;
        }
        out.println("addNewTask smt went wrong...");
    }

}
