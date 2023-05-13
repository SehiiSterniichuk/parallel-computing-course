package lab4;

import java.io.*;

public interface MatrixLoader {

    static void write(DataOutputStream out, double[][] data, String client) throws IOException {
        long start = System.nanoTime();
        int size = data.length;
        for (int i = 0; i < size; i++) {
            double[] row = data[i];
            for (double v : row) {
                out.writeDouble(v);
            }
            if (size >= 2000 && i % 1000 == 0) {
                System.out.printf("Writing matrix of the size: %d, row: %d\n", size, i);
            }
        }
        long finish = (System.nanoTime() - start) / 1000;
        System.out.println("Time to write: " + finish + " " + client);
    }

    static double[][] read(DataInputStream in, int size, String client) throws IOException {
        double[][] array = new double[size][];
        long start = System.nanoTime();
        for (int i = 0; i < size; i++) {
            double[] doubleRow = new double[size];
            for (int j = 0; j < size; j++) {
                double v = in.readDouble();
                doubleRow[j] = v;
            }
            if (size >= 2000 && i % 1000 == 0) {
                System.out.printf("Reading matrix of the size: %d, row: %d\n", size, i);
            }
            array[i] = doubleRow;
        }
        long finish = (System.nanoTime() - start) / 1000;
        System.out.println("Time to read: " + finish + " " + client);
        return array;
    }
}
