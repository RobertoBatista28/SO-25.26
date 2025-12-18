package org.resources;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BaseDados {
    private final String nomeTabela;
    private final Lock lock = new ReentrantLock();

    public BaseDados(String nomeTabela) { this.nomeTabela = nomeTabela; }

    public void bloqueioLeitura() {
        try {
            lock.lockInterruptibly();
            System.out.println(" [DB] Tabela '" + nomeTabela + "' BLOQUEADA por " + Thread.currentThread().getName());
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void desbloquear() {
        if (((ReentrantLock)lock).isHeldByCurrentThread()) {
            lock.unlock();
            System.out.println(" [DB] Tabela '" + nomeTabela + "' LIBERTADA por " + Thread.currentThread().getName());
        }
    }
}