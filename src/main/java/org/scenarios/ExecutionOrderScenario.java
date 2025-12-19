package org.scenarios;

import org.monitor.MonitorEBPF;

public class ExecutionOrderScenario {
    public static void run() {
        System.out.println("\n[CENÁRIO] ORDEM DE EXECUÇÃO (Anestesia vs Cirurgia)");
        MonitorEBPF monitor = MonitorEBPF.getInstance();

        Thread t1 = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            monitor.registarAcesso(Thread.currentThread(), "Procedimento_Anestesia");
            System.out.println("Anestesia: Aplicada.");
        }, "Anestesia");

        Thread t2 = new Thread(() -> {
            monitor.registarAcesso(Thread.currentThread(), "Procedimento_Cirurgia");
            System.out.println("Cirurgia: Incisão feita.");
        }, "Cirurgia");

        monitor.track(t1);
        monitor.track(t2);

        // Erro propositado: Iniciar sem coordenação
        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("(Verifique no log se a Cirurgia ocorreu antes da Anestesia)");
        monitor.untrack(t1);
        monitor.untrack(t2);
    }
}