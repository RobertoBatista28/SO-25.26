package org.scenarios;

import org.monitor.MonitorEBPF;

public class StarvationScenario {
    public static void run() {
        System.out.println("\n[CENÁRIO] STARVATION (Injustiça)");
        MonitorEBPF monitor = MonitorEBPF.getInstance();
        Object recurso = new Object();

        Thread vitima = new Thread(() -> {
            synchronized (recurso) {
                System.out.println("Vítima: Consegui!");
            }
        }, "Vitima_Baixa_Prio");
        vitima.setPriority(Thread.MIN_PRIORITY);
        monitor.track(vitima);

        System.out.println("A lançar flood de alta prioridade...");
        for (int i = 0; i < 500; i++) {
            Thread spam = new Thread(() -> {
                synchronized (recurso) {
                    try {
                        Thread.sleep(10);
                    } catch(InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            spam.setPriority(Thread.MAX_PRIORITY);
            spam.start();
        }

        vitima.start();
        try {
            Thread.sleep(5000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        monitor.untrack(vitima);
    }
}