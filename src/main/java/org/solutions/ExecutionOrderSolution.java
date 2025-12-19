package org.solutions;
import java.util.concurrent.Semaphore;

public class ExecutionOrderSolution {
    public static void run() {
        System.out.println("\n[SOLUÇÃO] ORDEM DE EXECUÇÃO (Sinalização com Semáforos)");
        
        // Inicializado a 0: Ninguém passa até haver um release()
        // Isto força a espera (bloqueio) inicial.
        Semaphore sinalizacao = new Semaphore(0);

        Thread anestesia = new Thread(() -> {
            System.out.println("Anestesia: A administrar...");
            try { 
                Thread.sleep(1500); 
            } catch (InterruptedException e) {
                System.err.println("Erro: Thread de anestesia interrompida.");
            }
            
            System.out.println("Anestesia: Completa. A sinalizar cirurgia...");
            
            // Permite que a outra thread prossiga
            sinalizacao.release(); 
        });

        Thread cirurgia = new Thread(() -> {
            try {
                System.out.println("Cirurgia: A aguardar anestesia...");
                
                // Só avança quando a anestesia fizer release()
                sinalizacao.acquire(); 
                
                System.out.println("Cirurgia: Corte seguro realizado.");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt(); 
            }
        });

        // A coordenação é interna, garantida pelo Semáforo.
        anestesia.start();
        cirurgia.start();

        // Apenas para garantir que o menu não volta imediatamente (opcional)
        try { 
            anestesia.join(); 
            cirurgia.join(); 
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}