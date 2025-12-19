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
        try {
            // [Monitor] Registar que a thread quer o recurso
            MonitorEBPF.getInstance().getDetector().registarEspera(Thread.currentThread(), lock);
            MonitorEBPF.getInstance().registarInicioEspera(Thread.currentThread(), "Equipamento:" + nome);
            
            lock.lockInterruptibly();
            
            // [Monitor] Registar que a thread obteve o recurso
            MonitorEBPF.getInstance().getDetector().registarAlocacao(Thread.currentThread(), lock);
            MonitorEBPF.getInstance().registarAcesso(Thread.currentThread(), "Equipamento:" + nome);
            
            System.out.println(" -> " + Thread.currentThread().getName() + " bloqueou o " + nome);
        } catch (InterruptedException e) {
            System.out.println(" -> " + Thread.currentThread().getName() + " foi interrompido no " + nome);
            Thread.currentThread().interrupt(); 
        }
    }

    public void libertar() {
        if (((ReentrantLock)lock).isHeldByCurrentThread()) {
            // [Monitor] Registar libertação
            MonitorEBPF.getInstance().getDetector().registarLibertacao(Thread.currentThread(), lock);
            
            lock.unlock();
            System.out.println(" -> " + Thread.currentThread().getName() + " libertou o " + nome);
        }
    }

    public String getNome() { return nome; }
}