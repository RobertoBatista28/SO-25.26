package org.scenarios;

import org.monitor.MonitorEBPF;

public class StarvationScenario {
    public static void run() {
        System.out.println("\n[CENÁRIO] STARVATION (Service Delay)");
        MonitorEBPF monitor = MonitorEBPF.getInstance();
        Object recurso = new Object(); // Recurso intrínseco para teste

        // Thread Vítima (Baixa Prioridade)
        Thread vitima = new Thread(() -> {
            System.out.println("Vítima: Tentar obter recurso...");
            synchronized (recurso) {
                monitor.registarAcesso(Thread.currentThread(), "Recurso_Unico");
                System.out.println("Vítima: Consegui!");
            }
        }, "Vitima_Baixa_Prio");

        vitima.setPriority(Thread.MIN_PRIORITY);
        monitor.track(vitima);

        System.out.println("A lançar flood de alta prioridade...");

        // Criar spam suficiente para bloquear o recurso por > Config.STARVATION_THRESHOLD_MS
        for (int i = 0; i < 300; i++) {
            Thread spam = new Thread(() -> {
                synchronized (recurso) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            spam.setPriority(Thread.MAX_PRIORITY);
            spam.start();
        }

        vitima.start();

        // Aguardar tempo suficiente para o Monitor detetar
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        monitor.untrack(vitima);
    }
}