package org.resources;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EquipamentoMedico {
    private final String nome;
    private final Lock lock = new ReentrantLock();

    public EquipamentoMedico(String nome) {
        this.nome = nome;
    }

    public void usar() {
        // MUDANÇA IMPORTANTE: lockInterruptibly permite que a thread acorde se levarmos um "interrupt"
        try {
            lock.lockInterruptibly();
            System.out.println(" -> " + Thread.currentThread().getName() + " bloqueou o " + nome);
        } catch (InterruptedException e) {
            System.out.println(" -> " + Thread.currentThread().getName() + " foi interrompido enquanto tentava pegar o " + nome);
            Thread.currentThread().interrupt(); // Repor estado de interrupção
        }
    }

    public void libertar() {
        // Só libertamos se tivermos o lock
        if (((ReentrantLock)lock).isHeldByCurrentThread()) {
            lock.unlock();
            System.out.println(" -> " + Thread.currentThread().getName() + " libertou o " + nome);
        }
    }

    public String getNome() { return nome; }
}
