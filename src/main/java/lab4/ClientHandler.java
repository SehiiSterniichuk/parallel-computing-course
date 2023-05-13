package lab4;

import lab1.Matrix;
import lab4.model.ResponseType;
import lab4.model.header.HeaderParametersHolder;
import lab4.model.header.NewTaskParameter;
import lab4.model.header.Prefix;
import lab4.model.header.TaskId;
import lab4.model.Header;
import lab4.model.Status;
import lab4.model.Task;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static lab4.HeaderReader.readHeader;
import static lab4.model.ResponseType.BAD_REQUEST;
import static lab4.model.ResponseType.OK;
import static lab4.Server.TIMEOUT;

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
             var socketWrapper = new SocketWrapper(clientSocket);
             var dOut = new DataOutputStream(clientSocket.getOutputStream());
             var inD = new DataInputStream(clientSocket.getInputStream())
        ) {
            socketWrapper.clientSocket.setSoTimeout(TIMEOUT);
            System.out.println("\nClient has connected to handler: " + handlerId);
            Header header = readHeader(in);
            System.out.println(header);
            switch (header.type()) {
                case POST_NEW_TASK -> addNewTask(out, header.parameters(), inD);
                case START_TASK -> startTask(out, header.parameters());
                case GET_TASK_STATUS -> getTaskStatus(out, header.parameters(), dOut);
                case GET_RESULT -> getTaskResult(out, header.parameters(), dOut);
                case BAD_REQUEST -> printlnBadRequest(out, "Undefined command");
                case SHUTDOWN -> shutdownCallback.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printlnBadRequest(PrintWriter out, String error) {
        out.println(BAD_REQUEST);
        out.println(Prefix.ERROR + error);
    }

    private void printfBadRequest(PrintWriter out, String format, Object... args) {
        printlnBadRequest(out, String.format(format, args));
    }


    private void startTask(PrintWriter out, HeaderParametersHolder parameters) {
        if (parameters instanceof TaskId(var id)) {
            Task task = getTask(out, id);
            if (task == null || task.getStatus() != Status.WAITING) {
                printfBadRequest(out, "Task %d not found or not started", id);
                return;
            }
            Future<Void> submit = taskExecutor.submit(() -> {
                task.run();
                return null;
            });
            task.setFuture(submit);

        }
        printlnBadRequest(out, "!(parameters instanceof TaskId(var id))");
    }

    private void addNewTask(PrintWriter out, HeaderParametersHolder parameters, DataInputStream in) throws IOException {
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
            return;
        }
        printlnBadRequest(out, "!(parameters instanceof NewTaskParameter)");
    }

    private long getNextId() {
        taskCounter.compareAndExchange(Long.MAX_VALUE, 0);
        return taskCounter.getAndIncrement();
    }

    private void getTaskStatus(PrintWriter out, HeaderParametersHolder parameters, DataOutputStream dOut) throws IOException {
        if (parameters instanceof TaskId(var id)) {
            Task task = getTask(out, id);
            if (task == null) {
                printfBadRequest(out, "Task %d not found", id);
                return;
            }
            Status status = task.getStatus();
            out.println(status);
            if(status == Status.DONE){
                out.println(OK);
                getCompletedTaskResult(out, task, dOut);
            }
        }
    }

    private void getTaskResult(PrintWriter out, HeaderParametersHolder parameters, DataOutputStream dOut) throws IOException {
        if (parameters instanceof TaskId(var id)) {
            Task task = getTask(out, id);
            if (task == null) return;
            Status status = task.getStatus();
            switch (status) {
                case DONE -> getCompletedTaskResult(out, task, dOut);
                case RUNNING -> waitAndGetTaskResult(out, task, dOut);
                case WAITING -> printfBadRequest(out, "Task %d has not yet been started to get it", id);
            }
            if (status == Status.DONE) map.remove(id);
            return;
        }
        String message = "!(parameters instanceof TaskId)";
        printlnBadRequest(out, message);
        System.err.println(message);
    }

    private void waitAndGetTaskResult(PrintWriter out, Task task, DataOutputStream dOut) throws IOException {
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
        sendResult(out, task.toString(), result, dOut);
    }

    private void getCompletedTaskResult(PrintWriter out, Task task, DataOutputStream dOut) throws IOException {
        Task.Result result;
        try {
            result = task.getResult();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        sendResult(out, task.toString(), result, dOut);
    }

    private void sendResult(PrintWriter out, String task, Task.Result result, DataOutputStream dOut) throws IOException {
        out.println(Prefix.TIME.v + result.timeOfExecution());
        System.out.println("Downloading the result of the task: " + task + " has started");
        MatrixLoader.write(dOut, result.data(), task);
        out.println("\r\n");
        System.out.println("Downloading the result of the task: " + task + " has finished");
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

    private record SocketWrapper(Socket clientSocket) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            clientSocket.close();
        }
    }
}
