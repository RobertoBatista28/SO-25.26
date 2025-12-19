package org.solutions;

import java.util.Scanner;

import org.resources.StockSangue;

public class RaceConditionSolution {
    public static void run(Scanner scanner) {
        System.out.println("\n[SOLUÇÃO] RACE CONDITION (Synchronized)");
        StockSangue stock = new StockSangue(10);
        int pedido = 8;

        Runnable r = () -> {
            // Usa o método seguro
            if (!stock.retirarSeguroManual(pedido))
                System.out.println("-> " + Thread.currentThread().getName() + ": Falhou (Stock Insuficiente).");
        };

        Thread t1 = new Thread(r, "Medico_1");
        Thread t2 = new Thread(r, "Medico_2");
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Stock Final: " + stock.getUnidades() + " (Consistente)");
    }
}