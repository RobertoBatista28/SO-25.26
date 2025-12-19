package org.resources;

import org.monitor.MonitorEBPF;

public class StockSangue {
    private int unidades;

    public StockSangue(int inicio) {
        this.unidades = inicio;
    }

    public synchronized void adicionar(int qtd) {
        // Hook de monitorização
        MonitorEBPF.getInstance().registarAcesso(Thread.currentThread(), "StockSangue(Escrita)");
        this.unidades += qtd;
        System.out.println("[STOCK] Adicionado " + qtd + ". Total: " + unidades);
    }

    // CORREÇÃO (Seguro): Usa synchronized para atomicidade
    public synchronized boolean retirarSeguroManual(int qtd) {
        // Hook de monitorização - Regista que a thread conseguiu entrar na secção crítica
        MonitorEBPF.getInstance().registarAcesso(Thread.currentThread(), "StockSangue(Leitura/Escrita)");

        if (unidades >= qtd) {
            unidades -= qtd;
            System.out.println("[STOCK] Retirado " + qtd + ". Restante: " + unidades);
            return true;
        }
        return false;
    }

    // FALHA (Inseguro): Simula latência para causar Race Condition
    public void retirarInseguro(int qtd) {
        // [eBPF Probe] Sinaliza entrada em zona de perigo
        MonitorEBPF.getInstance().probeUnsafeEnter("StockSangue:retirarInseguro");

        // Secção Crítica Vulnerável (Check-then-Act sem proteção)
        if (unidades >= qtd) {
            try {
                // Simula processamento para garantir que ocorre interleaving de threads
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[Aviso] Latência interrompida em retirarInseguro");
            }
            unidades -= qtd;
            System.out.println("-> " + Thread.currentThread().getName() + " retirou " + qtd + ". Stock: " + unidades);
        }
        
        // [eBPF Probe] Sinaliza saída da zona de perigo
        MonitorEBPF.getInstance().probeUnsafeExit("StockSangue:retirarInseguro");
    }

    public int getUnidades() {
        return unidades;
    }
}