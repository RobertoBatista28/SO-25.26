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
            }
            hist.bloqueioLeitura(); // Bloqueia aqui à espera de t2
            hist.desbloquear();
            pac.desbloquear();
        }, "Transacao_A");

        Thread t2 = new Thread(() -> {
            hist.bloqueioLeitura();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            pac.bloqueioLeitura(); // Bloqueia aqui à espera de t1
            pac.desbloquear();
            hist.desbloquear();
        }, "Transacao_B");

        monitor.track(t1);
        monitor.track(t2);

        System.out.println("Pressione ENTER para lançar deadlock...");
        if (scanner.hasNextLine())
            scanner.nextLine();

        t1.start();
        t2.start();

        System.out.println("A aguardar deteção pelo Monitor (5s)...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (t1.isAlive() || t2.isAlive()) {
            System.out.println("Sistema encravado! Forçando recuperação (Terminação)...");
            t1.interrupt();
            t2.interrupt();
        }

        monitor.untrack(t1);
        monitor.untrack(t2);

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}