package lab4.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class Client {
    static String host = "localhost";

    public static void main(String[] args) {
        System.out.println("Client has started");
        int port = 1234;
        try (Socket socket = new Socket(host, port)) {
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("GET_RESULT");
            out.println("id: " + 12);
            out.println("\r\n");
            String s = in.readLine();
            System.out.println(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
