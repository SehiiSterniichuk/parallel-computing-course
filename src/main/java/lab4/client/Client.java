package lab4.client;

import lab1.Matrix;
import lab4.server.MatrixLoader;
import lab4.config.RequestType;
import lab4.config.ResponseType;
import lab4.config.Status;
import lab4.config.Prefix;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

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
        try (Socket socket = new Socket(host, port);
             var dIn = new DataInputStream(socket.getInputStream());
             var dOut = new DataOutputStream(socket.getOutputStream());
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            work(in, out, dIn, dOut);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void work(BufferedReader in, PrintWriter out, DataInputStream dIn, DataOutputStream dOut) throws IOException {
        if (size < 1) {
            System.out.println("Killer client has been called");
            shutdownServer(in, out);
            return;
        }
        taskId = postTask(in, out, dOut);
        if (taskId < 0) {
            System.err.printf("Server doesn't accept matrix. size: %d, threads: %d", size, threadNumber);
            return;
        }
        sleep(2);
        boolean successStart = startTask(taskId, in, out);
        if (!successStart) {
            System.err.println("Failed to start. " + this);
            return;
        }
        System.out.println("Successful start. " + this);
        sleep(1);
        Result result;
        int i = 0;
        do {
            result = getStatusOrResult(taskId, size, in, out, dIn);
            if (result == null) {
                System.out.println("Result is not ready yet." + this);
            }
            sleep(1);
            i++;
        } while (result == null && i < 1);
        Matrix resultMatrix;
        if (result == null) {
            System.out.println("The result is not ready, but the client asks for it " + this);
            resultMatrix = requestResult(taskId, size, out, in, dIn).matrix;
        } else {
            resultMatrix = result.matrix;
        }
        printMatrix("\nResult received. " + this, resultMatrix);
    }

    private long postTask(BufferedReader in, PrintWriter out, DataOutputStream dOut) throws IOException {
        Matrix matrix = new Matrix(size);
        String hello = String.format("Client %d created matrix of the size: %d \n", id, size);
        printMatrix(hello, matrix);
        return writeTask(matrix, in, out, dOut);
    }

    private static void sleep(long time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    record Result(Matrix matrix, long executionTime) {
    }

    private Result requestResult(long taskId, int size, PrintWriter out, BufferedReader in, DataInputStream dIn) {
        out.println(RequestType.GET_RESULT);
        out.println(Prefix.ID.v + taskId);
        out.println();
        return readResult(out, size, in, dIn);
    }

    private Result getStatusOrResult(long id, int size, BufferedReader in, PrintWriter out, DataInputStream dIn) {
        Status status = getStatus(id, in, out);
        return switch (status) {
            case WAITING, RUNNING -> null;
            case DONE -> readResult(out, size, in, dIn);
        };
    }

    private Result readResult(PrintWriter out, int size, BufferedReader in, DataInputStream dIn) {
        try {
            String line = in.readLine();
            ResponseType responseType = ResponseType.valueOf(line);
            return switch (responseType) {
                case BAD_REQUEST -> printBadRequestAndThrow(in, new IllegalStateException());
                case OK -> {
                    long executionTime = parseLong(in, Prefix.TIME);
                    System.out.println("Downloading the result: " + this + " executionTime: " + executionTime);
                    out.println(ResponseType.OK);
                    double[][] read = MatrixLoader.read(dIn, size, toString());
                    out.println(ResponseType.OK);
                    out.flush();
                    Matrix matrix = new Matrix(read);
                    yield new Result(matrix, executionTime);
                }
            };
        } catch (IOException e) {
            String string = this.toString();
            System.err.println(string);
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

    private void shutdownServer(BufferedReader in, PrintWriter out) {
        out.println(RequestType.SHUTDOWN);
        out.println();
        try {
            System.out.println("SHUTDOWN response: " + in.readLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        String readLine = null;
        try {
            readLine = in.readLine();
            substring = readLine.substring(p.v.length());
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println(readLine);
            throw e;
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
        out.println();
        try {
            String line;
            line = in.readLine();
            ResponseType responseType = ResponseType.valueOf(line);
            return switch (responseType) {
                case BAD_REQUEST -> printBadRequestAndThrow(in, new IllegalArgumentException());
                case OK -> Status.valueOf(in.readLine());
            };
        } catch (IOException e) {
            System.err.println("task: " + id + ". IOException in getStatus()");
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
}
