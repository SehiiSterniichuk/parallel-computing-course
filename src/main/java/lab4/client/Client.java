package lab4.client;

import lab1.Matrix;
import lab4.MatrixLoader;
import lab4.model.RequestType;
import lab4.model.ResponseType;
import lab4.model.Status;
import lab4.model.header.Prefix;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static java.lang.Math.min;

public class Client implements Runnable {
    private final String host;
    private final int port;

    private final int size;

    private final int id;

    private final int threadNumber;

    private static int clientCounter = 0;

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", size=" + size +
                ", threadNumber=" + threadNumber +
                '}';
    }

    public Client(String host, int port, int size, int threadNumber) {
        this.host = host;
        this.port = port;
        this.size = size;
        this.threadNumber = threadNumber;
        this.id = clientCounter++ % Integer.MAX_VALUE;
    }

    private static synchronized void printMatrix(String message, Matrix matrix) {
        System.out.println(message);
//        matrix.print();
    }

    private long taskId = -1;

    @Override
    public void run() {
        Matrix matrix = new Matrix(size);
        String hello = String.format("Client %d created matrix of the size: %d \n", id, size);
        printMatrix(hello, matrix);
        taskId = postTask(matrix);
        if (taskId < 0) {
            System.err.printf("Server doesn't accept matrix. size: %d, threads: %d", size, threadNumber);
            return;
        }
        sleep(1);
        boolean successStart = request((in, out) -> startTask(taskId, in, out));
        if (!successStart) {
            System.err.println("Failed to start. " + this);
            return;
        }
        System.out.println("Successful start. " + this);
        int min = min(size, threadNumber);
        sleep(min);
        Supplier<Status> getStatus = () -> request((in, out) -> getStatus(taskId, in, out));
        Status status = getStatus.get();
        for (int i = 0; status != Status.DONE && i < 3; ++i) {
            System.out.println("Result is not ready yet." + this);
            sleep(min);
            status = getStatus.get();
        }
        if (status != Status.DONE) {
            System.out.println("The result is not ready, but the client asks for it " + this);
        }
        Matrix result = getResult(id, size).matrix;
        printMatrix("Result received. " + this, result);
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

    record Result(Matrix matrix, long executionTime) {
    }

    private Result getResult(long id, int size) {
        try (Socket socket = new Socket(host, port);
             var dIn = new DataInputStream(socket.getInputStream());
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            return readResult(id, size, in, out, dIn);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long postTask(Matrix matrix) {
        try (Socket socket = new Socket(host, port);
             var dOut = new DataOutputStream(socket.getOutputStream());
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            return writeTask(matrix, in, out, dOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ResponseType getResponseType(BufferedReader in) {
        String line;
        try {
            line = in.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseType.valueOf(line);
    }

    private long writeTask(Matrix matrix, BufferedReader in, PrintWriter out, DataOutputStream dOut) throws IOException {
        out.println(RequestType.POST_NEW_TASK);
        out.println(Prefix.THREADS.v + threadNumber);
        out.println(Prefix.SIZE.v + matrix.size);
        out.println();
        ResponseType type = getResponseType(in);
        if (type == ResponseType.BAD_REQUEST) {
            try {
                System.err.println(in.readLine());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return -1;
        }
        MatrixLoader.write(dOut, matrix.data, toString());
        return parseLong(in, Prefix.ID);
    }

    private long parseLong(BufferedReader in, Prefix p) {
        String substring;
        try {
            substring = in.readLine().substring(p.v.length());
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
            ResponseType responseType = ResponseType.valueOf(line);
            return switch (responseType) {
                case BAD_REQUEST -> printBadRequestAndThrow(in, new IllegalArgumentException());
                case OK -> Status.valueOf(in.readLine());
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T printBadRequestAndThrow(BufferedReader in, RuntimeException e) {
        try {
            System.err.println(in.readLine());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        throw e;
    }

    private Result readResult(long id, int size, BufferedReader in, PrintWriter out, DataInputStream dIn) {
        out.println(RequestType.GET_RESULT);
        out.println(Prefix.ID.v + id);
        out.println("\r\n");
        try {
            String line = in.readLine();
            ResponseType responseType = ResponseType.valueOf(line);
            return switch (responseType) {
                case BAD_REQUEST -> printBadRequestAndThrow(in, new IllegalStateException());
                case OK -> {
                    long executionTime = parseLong(in, Prefix.TIME);
                    System.out.println(this + " executionTime: " + executionTime);
                    Matrix matrix = new Matrix(MatrixLoader.read(dIn, size, toString()));
                    yield new Result(matrix, executionTime);
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
