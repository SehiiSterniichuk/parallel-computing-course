package lab4.server;

import lab1.Matrix;
import lab4.config.ResponseType;
import lab4.server.model.header.HeaderParametersHolder;
import lab4.server.model.header.NewTaskParameter;
import lab4.config.Prefix;
import lab4.server.model.header.TaskId;
import lab4.server.model.Header;
import lab4.config.Status;
import lab4.server.model.Task;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static lab4.server.HeaderReader.readHeader;
import static lab4.server.Server.TIMEOUT;
import static lab4.config.ResponseType.BAD_REQUEST;
import static lab4.config.ResponseType.OK;

public class ClientHandler implements Runnable {
    private static final AtomicLong taskCounter = new AtomicLong(0);
    private static final int WAIT_RESULT_TIME = 15;
    private static final TimeUnit WAIT_RESULT_UNITS = TimeUnit.MINUTES;
    private final Socket clientSocket;
    private final ExecutorService taskExecutor;
    private final ConcurrentHashMap<Long, Task> map;
    private final Runnable shutdownCallback;
    private static long handlerCounter = 0;
    private final long handlerId;

    public ClientHandler(Socket clientSocket, ExecutorService taskExecutor, ConcurrentHashMap<Long, Task> map, Runnable shutdownCallback) {
        this.clientSocket = clientSocket;
        this.taskExecutor = taskExecutor;
        this.map = map;
        this.shutdownCallback = shutdownCallback;
        this.handlerId = (handlerCounter++) % Long.MAX_VALUE;
    }


    @Override
    public void run() {
        try (var in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             var out = new PrintWriter(clientSocket.getOutputStream(), true);
             var dOut = new DataOutputStream(clientSocket.getOutputStream());
             var inD = new DataInputStream(clientSocket.getInputStream())
        ) {
            System.out.println("\nClient has connected to handler: " + handlerId);
            boolean isWorking = true;
            clientSocket.setSoTimeout(TIMEOUT);
            while (isWorking) {
                System.out.println();
                Header header;
                try {
                    header = readHeader(in);
                } catch (RuntimeException e) {
                    printlnBadRequest(out, e.getMessage());
                    return;
                }
                System.out.println(header);
                isWorking = switch (header.type()) {
                    case POST_NEW_TASK -> addNewTask(out, header.parameters(), inD);
                    case START_TASK -> startTask(out, header.parameters());
                    case GET_TASK_STATUS -> getTaskStatus(in, out, header.parameters(), dOut);
                    case GET_RESULT -> getTaskResult(in, out, header.parameters(), dOut);
                    case BAD_REQUEST -> printlnBadRequest(out, "Undefined command");
                    case SHUTDOWN -> {
                        out.println(OK);
                        shutdownCallback.run();
                        yield false;
                    }
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean printlnBadRequest(PrintWriter out, String error) {
        out.println(BAD_REQUEST);
        out.println(Prefix.ERROR + error);
        return false;
    }

    private void printfBadRequest(PrintWriter out, String format, Object... args) {
        printlnBadRequest(out, String.format(format, args));
    }


    private boolean startTask(PrintWriter out, HeaderParametersHolder parameters) {
        if (parameters instanceof TaskId(var id)) {
            Task task = getTask(out, id);
            if (task == null || task.getStatus() != Status.WAITING) {
                printfBadRequest(out, "Task %d not found or not started", id);
                return false;
            }
            Future<Void> submit = taskExecutor.submit(() -> {
                task.run();
                return null;
            });
            task.setFuture(submit);
            return true;
        }
        printlnBadRequest(out, "!(parameters instanceof TaskId(var id))");
        return false;
    }

    private boolean addNewTask(PrintWriter out, HeaderParametersHolder parameters, DataInputStream in) throws IOException {
        if (parameters instanceof NewTaskParameter(int size, int numberOfThreads)) {
            out.println(OK);
            System.out.println("Started reading matrix of the size: " + size);
            double[][] array;
            array = MatrixLoader.read(in, size, "Matrix of the size: " + size);
            System.out.println("Finished reading matrix of the size: " + size);
            long id = getNextId();
            Task newTask = new Task(new Matrix(array), id, numberOfThreads);
            map.put(id, newTask);
            out.println(Prefix.ID.v + id);
            return true;
        }
        printlnBadRequest(out, "!(parameters instanceof NewTaskParameter)");
        return false;
    }

    private long getNextId() {
        taskCounter.compareAndExchange(Long.MAX_VALUE, 0);
        return taskCounter.getAndIncrement();
    }

    private boolean getTaskStatus(BufferedReader in, PrintWriter out, HeaderParametersHolder parameters, DataOutputStream dOut) throws IOException {
        if (!(parameters instanceof TaskId(var id))) {
            printlnBadRequest(out, "!(parameters instanceof TaskId(var id))");
            return false;
        }
        Task task = getTask(out, id);
        if (task == null) {
            printfBadRequest(out, "Task %d not found", id);
            return false;
        }
        Status status = task.getStatus();
        out.println(status);
        System.out.println(status);
        if (status == Status.DONE) {
            out.println(OK);
            getCompletedTaskResult(in, out, task, dOut);
        }
        return true;
    }

    private boolean getTaskResult(BufferedReader in, PrintWriter out, HeaderParametersHolder parameters, DataOutputStream dOut) throws IOException {
        if (!(parameters instanceof TaskId(var id))) {
            String message = "!(parameters instanceof TaskId)";
            printlnBadRequest(out, message);
            System.err.println(message);
            return false;
        }
        Task task = getTask(out, id);
        if (task == null) return false;
        Status status = task.getStatus();
        switch (status) {
            case DONE -> getCompletedTaskResult(in, out, task, dOut);
            case RUNNING -> waitAndGetTaskResult(in, out, task, dOut);
            case WAITING -> printfBadRequest(out, "Task %d has not yet been started to get it", id);
        }
        boolean isDownloaded = task.getStatus() == Status.DONE;
        if (isDownloaded) map.remove(id);
        return !isDownloaded;
    }

    private void waitAndGetTaskResult(BufferedReader in, PrintWriter out, Task task, DataOutputStream dOut) throws IOException {
        Task.Result result;
        System.out.println("Started async wait for: " + task);
        long start = System.nanoTime();
        try {
            result = task.getResult(WAIT_RESULT_TIME, WAIT_RESULT_UNITS);
        } catch (TimeoutException e) {
            String message = "Timeout waiting for " + task;
            System.err.println(message);
            printlnBadRequest(out, message);
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long time = (System.nanoTime() - start) / 1000;
        System.out.println("Finished async wait for: " + task + " waiting time: " + time);
        sendResult(in, out, task.toString(), result, dOut);
    }

    private void getCompletedTaskResult(BufferedReader in, PrintWriter out, Task task, DataOutputStream dOut) throws IOException {
        Task.Result result;
        try {
            result = task.getResult();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        sendResult(in, out, task.toString(), result, dOut);
    }

    private void sendResult(BufferedReader in, PrintWriter out, String task, Task.Result result, DataOutputStream dOut) throws IOException {
        out.println(Prefix.TIME.v + result.timeOfExecution());
        ResponseType type = ResponseType.valueOf(in.readLine());
        isOk(type, task, " has started", "Client not ready. ");
        MatrixLoader.write(dOut, result.data(), task);
        type = ResponseType.valueOf(in.readLine());
        isOk(type, task, " has finished", "Bad download. ");
    }

    private static void isOk(ResponseType type, String task, String status, String error) {
        if (type == OK) {
            System.out.println("Downloading the result of the task: " + task + status);
        } else {
            throw new IllegalStateException(error + task);
        }
    }

    private Task getTask(PrintWriter out, long id) {
        Task task = map.get(id);
        if (task == null) {
            String message = "There is no task: " + id;
            printlnBadRequest(out, message);
            System.err.println(message);
        } else {
            out.println(OK);
        }
        return task;
    }
}
