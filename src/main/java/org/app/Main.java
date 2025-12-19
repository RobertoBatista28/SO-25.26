package org.app;

import java.util.Scanner;

import org.monitor.MonitorEBPF;
import org.resources.StockSangue;
import org.scenarios.DeadlockScenario;
import org.scenarios.ExecutionOrderScenario;
import org.scenarios.RaceConditionScenario;
import org.scenarios.StarvationScenario;
import org.solutions.DeadlockSolution;
import org.solutions.ExecutionOrderSolution;
import org.solutions.RaceConditionSolution;
import org.solutions.StarvationSolution;

public class Main {
    // Recurso global para testes manuais
    static StockSangue stockGlobal = new StockSangue(10);
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        MonitorEBPF monitor = MonitorEBPF.getInstance();
        monitor.start();

        while (true) {
            System.out.println("\n==============================================");
            System.out.println("   SIMULADOR HOSPITALAR (Sistemas Operativos)");
            System.out.println("==============================================");
            System.out.println("1. MENU SEGURO (Correções)");
            System.out.println("2. MENU INSEGURO (Falhas)");
            System.out.println("3. Gestão de Stock (Manual)");
            System.out.println("0. Sair");
            System.out.print("Escolha: ");
            String op = scanner.nextLine();

            switch (op) {
                case "1":
                    menuSolucoes();
                    break;
                case "2":
                    menuProblemas();
                    break;
                case "3":
                    gestaoStockInterativa();
                    break;
                case "0":
                    System.out.println("\n=== RELATÓRIO FINAL ===");
                    monitor.logEstatisticasFinais();
                    monitor.shutdown();
                    System.out.println("A encerrar sistema...");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    System.exit(0);
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    private static void menuSolucoes() {
        System.out.println("\n--- SOLUÇÕES E CORREÇÕES ---");
        System.out.println("1. Stock de Sangue (Synchronized)");
        System.out.println("2. Aceder a BD de Pacientes (Ordenação de Recursos)");
        System.out.println("3. Atendimentos de Pacientes (Fair Lock)");
        System.out.println("4. Cirurgia (Ordem de Execução - Join)");
        System.out.println("0. Voltar");
        System.out.print("Escolha: ");
        String op = scanner.nextLine();

        switch (op) {
            case "1":
                RaceConditionSolution.run(scanner);
                break;
            case "2":
                DeadlockSolution.run();
                break;
            case "3":
                StarvationSolution.run();
                break;
            case "4":
                ExecutionOrderSolution.run();
                break;
            case "0":
                break;
        }
    }

    private static void menuProblemas() {
        System.out.println("\n--- CENÁRIOS DE FALHAS (ATAQUES) ---");
        System.out.println("1. Stock de Sangue (Race Condition - Corrupção de Stock)");
        System.out.println("2. Aceder a BD de Pacientes (Deadlock - DoS em Base de Dados)");
        System.out.println("3. Atendimentos de Pacientes (Starvation - Flood na Triagem)");
        System.out.println("4. Cirurgia (Ordem Conflituante - Erro Protocolo)");
        System.out.println("0. Voltar");
        System.out.print("Escolha: ");
        String op = scanner.nextLine();

        try {
            switch (op) {
                case "1":
                    RaceConditionScenario.run(scanner);
                    break;
                case "2":
                    DeadlockScenario.run(scanner);
                    break;
                case "3":
                    StarvationScenario.run();
                    break;
                case "4":
                    ExecutionOrderScenario.run();
                    break;
                case "0":
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void gestaoStockInterativa() {
        System.out.println("\n>> STOCK MANUAL");
        System.out.println("Stock Atual: " + stockGlobal.getUnidades());
        System.out.println("1. Adicionar | 2. Retirar | 0. Voltar");
        String acao = scanner.nextLine();
        try {
            if (acao.equals("1")) {
                System.out.print("Qtd: ");
                stockGlobal.adicionar(Integer.parseInt(scanner.nextLine()));
            } else if (acao.equals("2")) {
                System.out.print("Qtd: ");
                if (!stockGlobal.retirarSeguroManual(Integer.parseInt(scanner.nextLine())))
                    System.out.println("Erro: Stock insuficiente.");
            }
        } catch (Exception e) {
        }
    }
}