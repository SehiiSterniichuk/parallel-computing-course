package lab4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public interface MatrixLoader {

    static void write(PrintWriter out, double[][] data) {
        long start = System.nanoTime();
        for (double[] row : data) {
            for (int j = 0; j < row.length; j++) {
                out.print(row[j]);
                if (j < row.length - 1) {
                    out.print(",");
                }
            }
            out.println();
        }
        long finish = (System.nanoTime() - start) / 1000;
        System.out.println("Time to write: " + finish);
    }

    static double[][] read(BufferedReader in, int size) throws IOException {
        double[][] array = new double[size][];
        long start = System.nanoTime();
        for (int i = 0; i < size; i++) {
            String[] row = in.readLine().split(",");
            double[] doubleRow = new double[size];
            for (int j = 0; j < row.length; j++) {
                String value = row[j];
                doubleRow[j] = Double.parseDouble(value);
            }
            array[i] = doubleRow;
        }
        long finish = (System.nanoTime() - start) / 1000;
        System.out.println("Time to read: " + finish);
        return array;
    }
}
