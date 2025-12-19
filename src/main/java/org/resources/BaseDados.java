package org.resources;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.monitor.MonitorEBPF;

public class BaseDados {
    private final String nomeTabela;
    private final Lock lock = new ReentrantLock();

    public BaseDados(String nomeTabela) {
        this.nomeTabela = nomeTabela;
    }

    public void bloqueioLeitura() {
        try {
            // [Monitor] Registo no Grafo de Espera (Request Edge)
            MonitorEBPF.getInstance().getDetector().registarEspera(Thread.currentThread(), lock);
            MonitorEBPF.getInstance().registarInicioEspera(Thread.currentThread(), "DB:" + nomeTabela);

            lock.lockInterruptibly();
            
            // [Monitor] Registo no Grafo de Espera (Allocation Edge - já obteve)
            MonitorEBPF.getInstance().getDetector().registarAlocacao(Thread.currentThread(), lock);
            MonitorEBPF.getInstance().registarAcesso(Thread.currentThread(), "DB:" + nomeTabela);

            System.out.println(" [DB] Tabela '" + nomeTabela + "' BLOQUEADA por " + Thread.currentThread().getName());
        } catch (InterruptedException e) {
            System.err.println(" [ERRO] Interrupção ao aceder à tabela: " + nomeTabela);
            Thread.currentThread().interrupt();
        }
    }

    public void desbloquear() {
        if (((ReentrantLock) lock).isHeldByCurrentThread()) {
            // [Monitor] Limpeza do Grafo
            MonitorEBPF.getInstance().getDetector().registarLibertacao(Thread.currentThread(), lock);

            lock.unlock();
            System.out.println(" [DB] Tabela '" + nomeTabela + "' LIBERTADA por " + Thread.currentThread().getName());
        }
    }
}