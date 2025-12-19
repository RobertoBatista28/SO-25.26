package org.solutions;
import org.app.Config;
import java.util.concurrent.locks.ReentrantLock;

public class StarvationSolution {
    public static void run() {
        System.out.println("\n[SOLUÇÃO] STARVATION (Fair Lock)");
        // TRUE = Garante justiça (FIFO)
        ReentrantLock fairLock = new ReentrantLock(true);

        Thread vitima = new Thread(() -> {
            System.out.println("Vítima na fila...");
            fairLock.lock();
            try {
                System.out.println(">>> Vítima atendida!");
            } finally {
                fairLock.unlock();
            }
        });
        vitima.setPriority(Thread.MIN_PRIORITY);

        // Encher fila
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                fairLock.lock();
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    fairLock.unlock();
                }
            }).start();
        }

        vitima.start();
        try {
            vitima.join(Config.STARVATION_THRESHOLD_MS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}