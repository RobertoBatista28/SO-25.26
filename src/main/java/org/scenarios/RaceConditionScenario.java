package org.scenarios;
import java.util.Scanner;

import org.resources.StockSangue;

public class RaceConditionScenario {
    public static void run(Scanner scanner) {
        System.out.println("\n[CENÁRIO] RACE CONDITION (Ataque Inventário)");
        int inicial = 10;
        int pedido = 8;
        System.out.println("Stock: " + inicial + " | 2 Threads pedem: " + pedido);
        
        StockSangue stock = new StockSangue(inicial);
        Runnable r = () -> stock.retirarInseguro(pedido);

        Thread t1 = new Thread(r, "Atacante_1");
        Thread t2 = new Thread(r, "Atacante_2");
        t1.start(); t2.start();
        
        try { t1.join(); t2.join(); } catch(Exception e){}
        System.out.println("Stock Final: " + stock.getUnidades());
        if(stock.getUnidades() < 0) System.out.println("[RESULTADO] Falha Confirmada (Stock Negativo).");
    }
}