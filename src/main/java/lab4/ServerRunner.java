package lab4;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerRunner {
    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 1234;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            Server server = new Server(serverSocket);
            server.start();
            System.out.println("Server has finished the work");
        }
    }
}
