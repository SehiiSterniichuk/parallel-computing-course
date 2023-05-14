package lab4;

import lab4.model.header.NewTaskParameter;
import lab4.model.header.TaskId;
import lab4.model.Header;
import lab4.model.RequestType;

import java.io.BufferedReader;
import java.io.IOException;

import static lab4.model.header.Prefix.*;

public interface HeaderReader {


    static Header readHeader(BufferedReader in) throws IOException {
        String input = in.readLine();
        RequestType type = getRequestType(input);
        int size = -1;
        int numberOfThreads = -1;
        long id = -1;
        while (RequestType.BAD_REQUEST != type && (input = in.readLine()) != null) {
            if (input.startsWith(SIZE.v)) {
                size = parseInteger(SIZE.v, input);
                continue;
            }
            if (input.startsWith(THREADS.v)) {
                numberOfThreads = parseInteger(THREADS.v, input);
                continue;
            }
            if (input.startsWith(ID.v)) {
                id = parseInteger(ID.v, input);
                continue;
            }
            if (input.isEmpty()) break;
            System.out.println("Unexpected parameters: " + input);
        }
        return switch (type) {
            case GET_RESULT, GET_TASK_STATUS, START_TASK -> getHeaderWithTaskId(type, id);
            case POST_NEW_TASK -> getHeaderWithNewTaskParams(size, numberOfThreads);
            case BAD_REQUEST, SHUTDOWN -> new Header(type, null);
        };
    }

    static Header getHeaderWithNewTaskParams(int size, int numberOfThreads) {
        lab1.Main.checkParameters(size, numberOfThreads);
        return new Header(RequestType.POST_NEW_TASK, new NewTaskParameter(size, numberOfThreads));
    }

    static Header getHeaderWithTaskId(RequestType type, long id) {
        if (id < 0) {
            return new Header(type, null);
        }
        return new Header(type, new TaskId(id));
    }

    static RequestType getRequestType(String input) {
        RequestType type;
        try {
            type = RequestType.valueOf(input);
        } catch (IllegalArgumentException e) {
            type = RequestType.BAD_REQUEST;
        }
        return type;
    }

    static int parseInteger(String prefix, String input) {
        return Integer.parseInt(input.substring(prefix.length()));
    }
}
