package org.scenarios;

import java.util.Scanner;

import org.monitor.MonitorEBPF;
import org.resources.BaseDados;

public class DeadlockScenario {
    public static void run(Scanner scanner) {
        System.out.println("\n[CENÁRIO] DEADLOCK (DoS BD)");
        BaseDados pac = new BaseDados("Pacientes");
        BaseDados hist = new BaseDados("Historico");
        MonitorEBPF monitor = MonitorEBPF.getInstance();

        Thread t1 = new Thread(() -> {
            pac.bloqueioLeitura();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Transacao_A interrompida.");
            }
            hist.bloqueioLeitura(); // Bloqueia
            hist.desbloquear();
            pac.desbloquear();
        }, "Transacao_A");

        Thread t2 = new Thread(() -> {
            hist.bloqueioLeitura();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Transacao_B interrompida.");
            }
            pac.bloqueioLeitura(); // Bloqueia
            pac.desbloquear();
            hist.desbloquear();
        }, "Transacao_B");

        monitor.track(t1);
        monitor.track(t2);
        System.out.println("Pressione ENTER para lançar deadlock...");
        scanner.nextLine();
        t1.start();
        t2.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } // Espera monitor detetar
        if (t1.isAlive()) {
            System.out.println("Sistema encravado! A matar processos...");
            t1.interrupt();
            t2.interrupt();
        }
        monitor.untrack(t1);
        monitor.untrack(t2);
    }
}