package org.app;

public class ThreadBase extends Thread {

    public ThreadBase(String name) {
        super(name);
    }

    public ThreadBase(Runnable target, String name) {
        super(target, name);
    }

    // Método utilitário para dormir sem ter de escrever try-catch sempre
    public static void sleepSafe(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
