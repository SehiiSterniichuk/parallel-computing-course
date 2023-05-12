package lab4;

import lab1.Matrix;
import lab4.header.parameters.HeaderParametersHolder;
import lab4.header.parameters.NewTaskParameter;
import lab4.header.parameters.Prefix;
import lab4.header.parameters.TaskId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static lab4.HeaderReader.readHeader;
import static lab4.ResponseType.BAD_REQUEST;
import static lab4.ResponseType.OK;
import static lab4.Server.TIMEOUT;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ExecutorService executor;
    private final ConcurrentHashMap<Long, Task> map;
    private static final AtomicLong taskCounter = new AtomicLong(0);

    private static long handlerCounter = 0;
    private final long handlerId;

    public ClientHandler(Socket clientSocket, ExecutorService executor, ConcurrentHashMap<Long, Task> map) {
        this.clientSocket = clientSocket;
        this.executor = executor;
        this.map = map;
        this.handlerId = (handlerCounter++) % Long.MAX_VALUE;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             var socketWrapper = new SocketWrapper(clientSocket)
        ) {
            socketWrapper.clientSocket.setSoTimeout(TIMEOUT);
            System.out.println("\nClient has connected to handler: " + handlerId);
            Header header = readHeader(in);
            System.out.println(header);
            switch (header.type()) {
                case POST_NEW_TASK -> addNewTask(in, out, header.parameters());
                case START_TASK -> startTask(out, header.parameters());
                case GET_RESULT -> getTaskResult(out, header.parameters());
                case GET_TASK_STATUS -> getTaskStatus(out, header.parameters());
                case BAD_REQUEST -> out.println(BAD_REQUEST);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void startTask(PrintWriter out, HeaderParametersHolder parameters) {
        if (parameters instanceof TaskId(var id)) {
            Task task = getTask(out, id);
            if (task == null || task.getStatus() == Status.RUNNING) {
                out.println(BAD_REQUEST);
                return;
            }
            executor.submit(task);
        }
        out.println(BAD_REQUEST);
    }

    private void addNewTask(BufferedReader in, PrintWriter out, HeaderParametersHolder parameters) throws IOException {
        if (parameters instanceof NewTaskParameter(int size, int numberOfThreads)) {
            out.println(OK);
            System.out.println("Started reading matrix of the size: " + size);
            double[][] array = MatrixLoader.read(in, size);
            System.out.println("Finished reading matrix of the size: " + size);
            long id = getNextId();
            Task newTask = new Task(new Matrix(array), id, numberOfThreads);
            map.put(id, newTask);
            out.println(Prefix.ID.v + id);
            return;
        }
        out.println(BAD_REQUEST);
    }

    private long getNextId() {
        taskCounter.compareAndExchange(Long.MAX_VALUE, 0);
        return taskCounter.getAndIncrement();
    }

    private void getTaskStatus(PrintWriter out, HeaderParametersHolder parameters) {
        if (parameters instanceof TaskId(var id)) {
            Task task = getTask(out, id);
            if (task == null) {
                out.println(BAD_REQUEST);
                return;
            }
            out.println(task.getStatus());
        }
    }

    private void getTaskResult(PrintWriter out, HeaderParametersHolder parameters) {
        if (parameters instanceof TaskId(var id)) {
            Task task = getTask(out, id);
            if (task == null) return;
            long timeOfExecution = task.getTimeOfExecution();
            out.println("timeOfExecution: " + timeOfExecution);
            System.out.println("Downloading the result of the task: " + id + " has started");
            MatrixLoader.write(out, task.getMatrix().data);
            out.println("\r\n");
            System.out.println("Downloading the result of the task: " + id + " has finished");
            map.remove(id);
            return;
        }
        out.println("getTaskResult smt went wrong...");
    }

    private Task getTask(PrintWriter out, long id) {
        Task task = map.get(id);
        if (task == null) {
            String message = "There is no task: " + id;
            out.println(BAD_REQUEST);
            System.out.println(message);
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
