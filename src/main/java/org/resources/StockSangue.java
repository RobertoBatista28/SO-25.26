package org.resources;

public class StockSangue {
    private int unidades;

    public StockSangue(int inicio) { this.unidades = inicio; }

    public synchronized void adicionar(int qtd) {
        this.unidades += qtd;
        System.out.println("[STOCK] Adicionado " + qtd + ". Total: " + unidades);
    }

    // CORREÇÃO (Safe): Usa synchronized para atomicidade
    public synchronized boolean retirarSeguroManual(int qtd) {
        if (unidades >= qtd) {
            unidades -= qtd;
            System.out.println("[STOCK] Retirado " + qtd + ". Restante: " + unidades);
            return true;
        }
        return false;
    }

    // FALHA (Unsafe): Simula latência para causar Race Condition
    public void retirarInseguro(int qtd) {
        // Secção Crítica Vulnerável
        if (unidades >= qtd) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            unidades -= qtd;
            System.out.println("-> " + Thread.currentThread().getName() + " retirou " + qtd + ". Stock: " + unidades);
        }
    }

    public int getUnidades() { return unidades; }
}