package lab4;

import lab4.header.parameters.NewTaskParameter;
import lab4.header.parameters.TaskId;

import java.io.BufferedReader;
import java.io.IOException;

public interface HeaderReader {
    String sizePrefix = "size: ";
    String numberOfThreadsPrefix = "number-of-threads: ";
    String idPrefix = "id: ";

    static Header readHeader(BufferedReader in) throws IOException {
        String input = in.readLine();
        RequestType type = getRequestType(input);
        int size = -1;
        int numberOfThreads = -1;
        long id = -1;
        while (RequestType.BAD_REQUEST != type && (input = in.readLine()) != null) {
            if (input.startsWith(sizePrefix)) {
                size = parseInteger(sizePrefix, input);
                continue;
            }
            if (input.startsWith(numberOfThreadsPrefix)) {
                numberOfThreads = parseInteger(numberOfThreadsPrefix, input);
                continue;
            }
            if (input.startsWith(idPrefix)) {
                id = parseInteger(idPrefix, input);
                continue;
            }
            if (input.isEmpty()) break;
            System.out.println("Unexpected parameters: " + input);
        }
        return switch (type) {
            case GET_RESULT, GET_TASK_STATUS -> getHeaderWithTaskId(type, id);
            case POST_NEW_TASK -> getHeaderWithNewTaskParams(type, size, numberOfThreads);
            case BAD_REQUEST -> new Header(type, null);
        };
    }

    static Header getHeaderWithNewTaskParams(RequestType type, int size, int numberOfThreads) {
        lab1.Main.checkParameters(size, numberOfThreads);
        return new Header(type, new NewTaskParameter(size, numberOfThreads));
    }

    static Header getHeaderWithTaskId(RequestType type, long id) {
        if (id < 0) throw new IllegalArgumentException("id is less than zero");
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
