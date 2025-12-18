package org.resources;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.monitor.MonitorEBPF;

public class BaseDados {
    private final String nomeTabela;
    private final Lock lock = new ReentrantLock();

    public BaseDados(String nomeTabela) { this.nomeTabela = nomeTabela; }

    public void bloqueioLeitura() {
        try {
            // solicitar recurso
            MonitorEBPF.getInstance().getDetector().registarEspera(Thread.currentThread(), lock);
            MonitorEBPF.getInstance().registarInicioEspera(Thread.currentThread(), "DB:" + nomeTabela);
            
            lock.lockInterruptibly(); // Tenta adquirir o recurso
            
            // utilizar recurso
            MonitorEBPF.getInstance().getDetector().registarAlocacao(Thread.currentThread(), lock);
            MonitorEBPF.getInstance().registarAcesso(Thread.currentThread(), "DB:" + nomeTabela);
            
            System.out.println(" [DB] Tabela '" + nomeTabela + "' BLOQUEADA por " + Thread.currentThread().getName());
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void desbloquear() {
        if (((ReentrantLock)lock).isHeldByCurrentThread()) {
            // libertar recurso
            MonitorEBPF.getInstance().getDetector().registarLibertacao(Thread.currentThread(), lock);
            
            lock.unlock();
            System.out.println(" [DB] Tabela '" + nomeTabela + "' LIBERTADA por " + Thread.currentThread().getName());
        }
    }
}