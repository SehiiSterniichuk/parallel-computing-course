package lab4.client;

import lab1.Matrix;
import lab4.MatrixLoader;
import lab4.RequestType;
import lab4.ResponseType;
import lab4.Status;
import lab4.header.parameters.Prefix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static java.lang.Math.max;

public class Client implements Runnable {
    private final String host;
    private final int port;

    private final int size;

    private final int id;

    private final int threadNumber;

    private static int clientCounter = 0;

    public Client(String host, int port, int size, int threadNumber) {
        this.host = host;
        this.port = port;
        this.size = size;
        this.threadNumber = threadNumber;
        this.id = clientCounter++ % Integer.MAX_VALUE;
    }

    @SuppressWarnings("unused")
    private static synchronized void printMatrix(String message, Matrix matrix) {
        System.out.println(message);
//        matrix.print();
    }

    @Override
    public void run() {
        Matrix matrix = new Matrix(size);
        String hello = String.format("Client %d created matrix of the size: %d \n", id, size);
        printMatrix(hello, matrix);
        long taskId = request((in, out) -> postNewTask(matrix, in, out));
        if (taskId < 0) {
            System.err.printf("Server doesn't accept matrix. size: %d, threads: %d", size, threadNumber);
            return;
        }
        sleep(1);
        boolean successStart = request((in, out) -> startTask(taskId, in, out));
        if (!successStart) {
            System.err.printf("Failed to start the task: %d. Client: %d, size: %d, threads: %d\n", taskId, id, size, threadNumber);
            return;
        }
        System.out.printf("Successful start of the task: %d, Client: %d, size: %d, threads: %d\n", taskId, id, size, threadNumber);
        int max = max(size, threadNumber);
        sleep(max);
        while (request((in, out) -> getStatus(taskId, in, out)) != Status.DONE) {
            System.out.printf("Result is not ready yet. task: %d. Client: %d, size: %d, threads: %d\n", taskId, id, size, threadNumber);
            sleep(max);
        }
        Matrix result = request((in, out) -> getResult(taskId, in, out, size));
        String byeBye = String.format("Client %d has got result of the task: %d, size: %d, threads: %d\n", id, taskId, size, threadNumber);
        printMatrix(byeBye, result);
    }

    private static void sleep(long time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T request(BiFunction<BufferedReader, PrintWriter, T> operation) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            return operation.apply(in, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ResponseType getResponseType(BufferedReader in) {
        String line;
        try {
            line = in.readLine();
        } catch (IOException | IllegalArgumentException e) {
            return ResponseType.BAD_REQUEST;
        }
        return ResponseType.valueOf(line);
    }

    private long postNewTask(Matrix matrix, BufferedReader in, PrintWriter out) {
        out.println(RequestType.POST_NEW_TASK);
        out.println(Prefix.THREADS.v + threadNumber);
        out.println(Prefix.SIZE.v + matrix.size);
        out.println();
        ResponseType type = getResponseType(in);
        if (type == ResponseType.BAD_REQUEST) {
            return -1;
        }
        MatrixLoader.write(out, matrix.data);
        String substring;
        try {
            substring = in.readLine().substring(Prefix.ID.v.length());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Long.parseLong(substring);
    }

    private boolean startTask(long id, BufferedReader in, PrintWriter out) {
        out.println(RequestType.START_TASK);
        out.println(Prefix.ID.v + id);
        out.println();
        ResponseType type = getResponseType(in);
        return type != ResponseType.BAD_REQUEST;
    }

    private Status getStatus(long id, BufferedReader in, PrintWriter out) {
        out.println(RequestType.GET_TASK_STATUS);
        out.println(Prefix.ID.v + id);
        out.println("\r\n");
        try {
            String line = in.readLine();
            System.out.println(line);//ok
            ResponseType responseType = ResponseType.valueOf(line);
            return switch (responseType) {
                case BAD_REQUEST -> throw new IllegalArgumentException();
                case OK -> Status.valueOf(in.readLine());
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Matrix getResult(long id, BufferedReader in, PrintWriter out, int size) {
        out.println(RequestType.GET_RESULT);
        out.println(Prefix.ID.v + id);
        out.println("\r\n");
        try {
            String line = in.readLine();
            System.out.println(line);//ok
            ResponseType responseType = ResponseType.valueOf(line);
            return switch (responseType) {
                case BAD_REQUEST -> throw new IllegalStateException();
                case OK -> {
                    in.readLine(); //read separator
                    yield new Matrix(MatrixLoader.read(in, size));
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
