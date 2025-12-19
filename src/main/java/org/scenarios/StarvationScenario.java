package org.scenarios;

import org.monitor.MonitorEBPF;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.List;

public class StarvationScenario {
    public static void run() {
        System.out.println("\n[CENÁRIO] STARVATION (Simulação de DoS)");
        System.out.println("Objetivo: O Monitor deve gerar alertas enquanto a Vítima está bloqueada.");

        MonitorEBPF monitor = MonitorEBPF.getInstance();

        // Lock INJUSTO (fair=false) é fundamental para permitir Starvation
        ReentrantLock lockInjusto = new ReentrantLock(false);

        // Lista para guardar threads de spam e poder matá-las no fim
        List<Thread> spamThreads = new ArrayList<>();

        // 1. Configurar a Vítima (Serviço Crítico)
        Thread vitima = new Thread(() -> {
            System.out.println("Vítima: Tentar adquirir recurso (Início da espera)...");

            // O Monitor vai detetar que esta thread fica 'presa' aqui
            lockInjusto.lock();
            try {
                // Se esta linha aparecer cedo, o cenário falhou
                System.out.println("Vítima: CONSEGUI o recurso após o spam ter terminado!");
                monitor.registarAcesso(Thread.currentThread(), "Recurso_Critico");
            } finally {
                lockInjusto.unlock();
            }
        }, "Vitima_Baixa_Prio");

        vitima.setPriority(Thread.MIN_PRIORITY);
        monitor.track(vitima); // Começa a vigiar

        // 2. Lançar o Flood (Ataque) - Loop Infinito
        System.out.println("Ataque: Iniciando flood de alta prioridade...");
        for (int i = 0; i < 300; i++) {
            Thread spam = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        lockInjusto.lock();
                        try {
                            // Ocupa o recurso (simula processamento)
                            Thread.sleep(10);
                        } finally {
                            lockInjusto.unlock();
                        }
                        // Re-adquire imediatamente (Greedy)
                    }
                } catch (InterruptedException e) {
                    // Thread interrompida, sair graciosamente
                    Thread.currentThread().interrupt();
                }
            });
            spam.setPriority(Thread.MAX_PRIORITY);
            spam.start();
            spamThreads.add(spam);
        }

        // Dá tempo ao flood para "agarrar" o recurso
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }

        // 3. Iniciar Vítima
        vitima.start();

        // 4. Janela de Observação (O Teste)
        System.out.println(">> A observar durante 10 segundos (Verifica o Log/Consola)...");
        try {
            // Se houver Starvation, ela NÃO termina e o join esgota o tempo (timeout).
            vitima.join(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 5. Verificação do Resultado e Limpeza
        if (vitima.isAlive()) {
            System.out.println("\n[SUCESSO] Passaram 10s e a Vítima continua bloqueada.");
            System.out.println("[SUCESSO] Cenário de Starvation/DoS demonstrado.");
            System.out.println("[INFO] A matar threads de spam...");

            // Primeiro matar o spam para libertar o recurso
            for (Thread t : spamThreads) {
                t.interrupt();
            }

            // Aguardar que as threads de spam terminem
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Agora a vítima deve conseguir obter o lock rapidamente
            System.out.println("[INFO] Spam terminado. A vítima deve agora conseguir prosseguir...");

            try {
                vitima.join(2000); // Dá 2 segundos para a vítima terminar
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Se ainda estiver viva, força a interrupção
            if (vitima.isAlive()) {
                vitima.interrupt();
            }
        } else {
            System.out.println("\n[FALHA] A Vítima conseguiu entrar. O ataque foi fraco.");
        }

        monitor.untrack(vitima);
    }
}