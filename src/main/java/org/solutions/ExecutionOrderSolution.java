package org.solutions;

public class ExecutionOrderSolution {
    public static void run() {
        System.out.println("\n[SOLUÇÃO] ORDEM DE EXECUÇÃO (Join)");
        Thread anestesia = new Thread(() -> {
            System.out.println("Anestesia: A administrar...");
            try{Thread.sleep(1500);}catch(Exception e){}
            System.out.println("Anestesia: Completa.");
        });
        Thread cirurgia = new Thread(() -> System.out.println("Cirurgia: Corte seguro realizado."));

        anestesia.start();
        try {
            // Garante que anestesia acaba antes da cirurgia começar
            anestesia.join();
            cirurgia.start();
        } catch(Exception e){}
    }
}