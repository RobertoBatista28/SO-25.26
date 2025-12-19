package org.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.app.Config; 

public class MonitorEBPF extends Thread {
    private static MonitorEBPF instance;
    private final List<Thread> threadsVigiadas = new ArrayList<>();
    private final LoggerMonitor logger;
    private final DetectorDeadlock detector;
    private volatile boolean running = true;

    // Estatísticas
    private final Map<Thread, Long> tempoInicioEspera = new ConcurrentHashMap<>();
    private final Map<String, Integer> contagemAcessos = new ConcurrentHashMap<>();
    private final List<String> ordemExclusividade = Collections.synchronizedList(new ArrayList<>());
    
    // Deteção de Race Condition (Contador de threads em zona crítica insegura)
    private final Map<String, Integer> threadsEmZonaInsegura = new ConcurrentHashMap<>();

    private MonitorEBPF() {
        this.logger = new LoggerMonitor(Config.LOG_FILE); 
        this.detector = new DetectorDeadlock();
        this.setName("Monitor-Security-Kernel");
    }

    public static synchronized MonitorEBPF getInstance() {
        if (instance == null) instance = new MonitorEBPF();
        return instance;
    }

    public synchronized void track(Thread t) { 
        if(!threadsVigiadas.contains(t)) threadsVigiadas.add(t); 
    }
    
    public synchronized void untrack(Thread t) { 
        threadsVigiadas.remove(t); 
        tempoInicioEspera.remove(t);
        detector.limparThread(t);
    }

    public DetectorDeadlock getDetector() { return detector; }
    
    public void shutdown() { 
        running = false; 
        logEstatisticasFinais();
        this.interrupt(); 
    }

    // --- PROBES / HOOKS (Simulação eBPF) ---

    // Chamado para registar sucesso na obtenção de recurso e manter estatísticas
    public void registarAcesso(Thread t, String recurso) {
        contagemAcessos.merge(t.getName(), 1, Integer::sum);
        String evento = String.format("[%d] %s obteve %s", System.currentTimeMillis(), t.getName(), recurso);
        ordemExclusividade.add(evento);
        logger.log("[ACESSO] " + t.getName() + " -> " + recurso);
    }

    // Chamado ANTES de entrar numa zona NÃO sincronizada (Deteção Race Condition)
    public synchronized void probeUnsafeEnter(String recursoID) {
        threadsEmZonaInsegura.merge(recursoID, 1, Integer::sum);
        
        // Se mais de 1 thread estiver na zona insegura ao mesmo tempo, é Race Condition
        if (threadsEmZonaInsegura.get(recursoID) > 1) {
            String msg = "[ALERTA CIBERSEGURANÇA] RACE CONDITION detetada em '" + recursoID + 
                         "'. Threads concorrentes: " + threadsEmZonaInsegura.get(recursoID);
            logger.log(msg); 
        }
    }

    // Chamado DEPOIS de sair da zona NÃO sincronizada
    public synchronized void probeUnsafeExit(String recursoID) {
        if (threadsEmZonaInsegura.containsKey(recursoID)) {
            int val = threadsEmZonaInsegura.get(recursoID) - 1;
            if (val <= 0) threadsEmZonaInsegura.remove(recursoID);
            else threadsEmZonaInsegura.put(recursoID, val);
        }
    }
    
    // Auxiliar para registar início de espera (para Starvation)
    public void registarInicioEspera(Thread t, String recurso) {
        tempoInicioEspera.putIfAbsent(t, System.currentTimeMillis());
    }

    // --- CICLO DE MONITORIZAÇÃO ---
    @Override
    public void run() {
        logger.log("[INFO] Monitor eBPF Iniciado.");
        while (running) {
            try {
                // 1. Deteção de Deadlock (Wait-for Graph - SO-T-05)
                long[] deadlockedIds = detector.detectar();
                if (deadlockedIds != null && deadlockedIds.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for(long id : deadlockedIds) sb.append(id).append(" ");
                    logger.log("[ALERTA CIBERSEGURANÇA] DEADLOCK (DoS) Confirmado! Threads IDs: " + sb.toString());
                    logger.log(detector.obterEstadoGrafo());
                }

                // 2. Deteção de Starvation
                synchronized (this) {
                    long agora = System.currentTimeMillis();
                    for (Thread t : threadsVigiadas) {
                        Thread.State estado = t.getState();
                        // Se thread está parada à espera de recurso (BLOCKED) ou notificação (WAITING)
                        if (estado == State.BLOCKED || estado == State.WAITING) {
                            tempoInicioEspera.putIfAbsent(t, agora);
                            long delta = agora - tempoInicioEspera.get(t);
                            
                            if (delta > Config.STARVATION_THRESHOLD_MS) {
                                logger.log("[ALERTA CIBERSEGURANÇA] STARVATION (Service Delay): " + t.getName() + 
                                           " em espera ha " + delta + "ms (Estado: " + estado + ")");
                            }
                        } else {
                            tempoInicioEspera.remove(t);
                        }
                    }
                }
                Thread.sleep(Config.MONITOR_INTERVAL_MS); 
            } catch (InterruptedException e) { 
                if(!running) break;
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void logEstatisticasFinais() {
        logger.log("\n=== ESTATÍSTICAS FINAIS ===");
        contagemAcessos.forEach((k,v) -> logger.log("Thread " + k + ": " + v + " acessos"));
        logger.log("--- Ordem de Exclusividade (Amostra) ---");
        synchronized(ordemExclusividade) {
            int max = Math.min(10, ordemExclusividade.size());
            for(int i=0; i<max; i++) logger.log(ordemExclusividade.get(i));
        }
    }
}