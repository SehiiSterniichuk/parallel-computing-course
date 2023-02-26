package lab1;

import java.util.Random;

public class Matrix {
    public final int size;
    public final double[][] data;

    private static final int MAX = 99;

    private static final Random rand = new Random();

    public Matrix(int size) {
        this.size = size;
        this.data = new double[size][size];
        randomInit(data);
    }

    private static void randomInit(double[][] data) {
        for (double[] array : data) {
            for (int j = 0; j < array.length; j++) {
                array[j] = rand.nextDouble() + rand.nextInt(MAX);
            }
        }
    }

    public void swapRows(int row1, int row2) {
        for (int i = 0; i < size; ++i) {
            double temp = data[row1][i];
            data[row1][i] = data[row2][i];
            data[row2][i] = temp;
        }
//        var temp = data[row1];
//        data[row1] = data[row2];
//        data[row2] = temp;
    }

    public void print() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.printf("%3.2f\t", data[i][j]);
            }
            System.out.println();
        }
    }
}
