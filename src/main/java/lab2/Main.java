package lab2;

import static lab2.Constants.*;
import static lab2.Printer.print;
import static lab2.Producer.getRandomExecutionTime;

public class Main {
    private static final int NUMBER_OF_LIFECYCLES = 4;//кількість ітерацій заповнення черги
    private static final int INTERRUPT_CYCLE_ID = -3;//ітерація на якій відбувається завершення роботи з покиданням активних задач.

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Program has started");
        ThreadPool threadPool = new ThreadPool(NUMBER_OF_THREADS);
        Producer producer = new Producer(threadPool);
        for (int i = 0; i < NUMBER_OF_LIFECYCLES; i++) {
            fillQueueInNewThread(producer);
            print("The queue has finished to fill");
            threadPool.execute();
            if (INTERRUPT_CYCLE_ID == i) {
                sleep(unit.toMillis(getRandomExecutionTime()));
                threadPool.interrupt();
                break;
            }
            if (NUMBER_OF_LIFECYCLES - 1 != i) {//якщо це не остання ітерація чекаємо щоб додати нові задачі у чергу
                waitThreadPoolToAddNewTasks(threadPool);
            }
        }
        if (INTERRUPT_CYCLE_ID < 0) {//умова при якій ми завершуємо роботу пулу з очікуванням на закінчення початих задач
            sleep(unit.toMillis(getRandomExecutionTime()));
            threadPool.terminate();
        }
        print(threadPool.getResult().toString());
    }

    private static void fillQueueInNewThread(Producer producer) throws InterruptedException {
        Thread thread = new Thread(producer::work);
        thread.start();
        sleep(Constants.unit.toMillis(QUEUE_FILLING_TIME));
        producer.stop();
        thread.join();
    }

    private static void waitThreadPoolToAddNewTasks(ThreadPool threadPool) throws InterruptedException {
        Object waiter = threadPool.getProducerWaiter();
        while (!threadPool.producerCanAddNewTasks()) {//чекаємо поки у пулі не стане пуста черга та не завершаться початі задачі
            synchronized (waiter) {
                waiter.wait();
            }
        }
        print("Producer can add new tasks");
    }

    static void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
