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
        // Registar acesso não sincronizado (para deteção)
        MonitorEBPF.getInstance().registarAcessoNaoSincronizado(Thread.currentThread(), "StockSangue:retirarInseguro");

        // Secção Crítica Vulnerável
        if (unidades >= qtd) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[Aviso] Latência interrompida em retirarInseguro");
            }
            unidades -= qtd;
            System.out.println("-> " + Thread.currentThread().getName() + " retirou " + qtd + ". Stock: " + unidades);
        }
    }

    public int getUnidades() {
        return unidades;
    }
}