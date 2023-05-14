package lab4.server.model.header;

public record NewTaskParameter(int size, int numberOfThreads) implements HeaderParametersHolder {
}
