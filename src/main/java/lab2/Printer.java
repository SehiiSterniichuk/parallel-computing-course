package lab2;

public class Printer {
    public static synchronized void print(String message) {
        System.out.println(message);
    }
}
