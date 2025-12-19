package org.scenarios;

import java.util.Scanner;
import org.monitor.MonitorEBPF;
import org.resources.StockSangue;

public class RaceConditionScenario {
    public static void run(Scanner scanner) {
        System.out.println("\n[CENÁRIO] RACE CONDITION (Ataque Inventário)");
        MonitorEBPF monitor = MonitorEBPF.getInstance();

        int inicial = 10;
        int pedido = 8;
        System.out.println("Stock: " + inicial + " | 2 Threads pedem: " + pedido);

        StockSangue stock = new StockSangue(inicial);
        Runnable r = () -> stock.retirarInseguro(pedido);

        Thread t1 = new Thread(r, "Medico_A");
        Thread t2 = new Thread(r, "Medico_B");

        // Tracking é essencial para o Monitor
        monitor.track(t1);
        monitor.track(t2);

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Stock Final: " + stock.getUnidades());
        if (stock.getUnidades() < 0) {
            System.out.println("[RESULTADO] Falha Confirmada (Stock Negativo). Ver log.");
        }

        monitor.untrack(t1);
        monitor.untrack(t2);
    }
}