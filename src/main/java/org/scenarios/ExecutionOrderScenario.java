package org.scenarios;

public class ExecutionOrderScenario {
    public static void run() {
        System.out.println("\n[CENÁRIO] ORDEM CONFLITUANTE");
        Thread anestesia = new Thread(() -> {
            try{Thread.sleep(2000);}catch(Exception e){}
            System.out.println("Anestesia: Completa.");
        });
        Thread cirurgia = new Thread(() -> System.out.println("Cirurgia: Corte realizado."));

        // Erro: Iniciar cirurgia sem esperar anestesia
        anestesia.start();
        cirurgia.start();
    }
}