package org.solutions;
import org.resources.BaseDados;

public class DeadlockSolution {
    public static void run() {
        System.out.println("\n[SOLUÇÃO] DEADLOCK (Ordenação de Recursos)");
        BaseDados p = new BaseDados("Pacientes");
        BaseDados h = new BaseDados("Historico");

        Runnable r = () -> {
            // ORDEM FIXA: Sempre P depois H
            p.bloqueioLeitura();
            try{Thread.sleep(10);}catch(Exception e){}
            h.bloqueioLeitura();
            System.out.println("Transação OK: " + Thread.currentThread().getName());
            h.desbloquear(); p.desbloquear();
        };

        Thread t1 = new Thread(r, "Solucao_A");
        Thread t2 = new Thread(r, "Solucao_B");
        t1.start(); t2.start();
        try { t1.join(); t2.join(); } catch(Exception e){}
    }
}