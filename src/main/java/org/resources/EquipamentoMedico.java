package org.resources;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.monitor.MonitorEBPF;

public class EquipamentoMedico {
    private final String nome;
    private final Lock lock = new ReentrantLock();

    public EquipamentoMedico(String nome) {
        this.nome = nome;
    }

    public void usar() {
        // MUDANÇA IMPORTANTE: lockInterruptibly permite que a thread acorde se levarmos um "interrupt"
        try {
            // Registar que a thread está à espera do recurso
            MonitorEBPF.getInstance().getDetector().registarEspera(Thread.currentThread(), lock);
            
            lock.lockInterruptibly();
            
            // Registar que a thread obteve o recurso (para deteção de deadlock)
            MonitorEBPF.getInstance().getDetector().registarAlocacao(Thread.currentThread(), lock);
            
            // Registar acesso para estatísticas (número de acessos + ordem)
            MonitorEBPF.getInstance().registarAcesso(Thread.currentThread(), "Equipamento:" + nome);
            
            System.out.println(" -> " + Thread.currentThread().getName() + " bloqueou o " + nome);
        } catch (InterruptedException e) {
            System.out.println(" -> " + Thread.currentThread().getName() + " foi interrompido enquanto tentava pegar o " + nome);
            Thread.currentThread().interrupt(); // Repor estado de interrupção
        }
    }

    public void libertar() {
        // Só libertamos se tivermos o lock
        if (((ReentrantLock)lock).isHeldByCurrentThread()) {
            // Registar libertação do recurso
            MonitorEBPF.getInstance().getDetector().registarLibertacao(Thread.currentThread(), lock);
            
            lock.unlock();
            System.out.println(" -> " + Thread.currentThread().getName() + " libertou o " + nome);
        }
    }

    public String getNome() { return nome; }
}
