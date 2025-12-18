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
    private final Map<Thread, Long> tempoEspera = new ConcurrentHashMap<>();
    private final Map<String, Integer> estatisticasAcessos = new ConcurrentHashMap<>();
    private final List<String> ordemObtencaoRecursos = Collections.synchronizedList(new ArrayList<>());

    private MonitorEBPF() {
        this.logger = new LoggerMonitor(Config.LOG_FILE);
        this.detector = new DetectorDeadlock();
        this.setName("Monitor-Security-Kernel");
    }

    public static synchronized MonitorEBPF getInstance() {
        if (instance == null) instance = new MonitorEBPF();
        return instance;
    }

    public synchronized void track(Thread t) { threadsVigiadas.add(t); }
    public synchronized void untrack(Thread t) { 
        threadsVigiadas.remove(t); 
        tempoEspera.remove(t); 
        detector.limparThread(t);
    }
    
    /**
     * Permite acesso ao detector para tracking académico de recursos.
     */
    public DetectorDeadlock getDetector() { 
        return detector; 
    }

    public void shutdown() { running = false; this.interrupt(); }

    /**
     * Hook estilo eBPF: Chamado pelos recursos quando uma thread ganha exclusividade.
     * Cumpre o requisito: "Registar todos os acessos... manter estatísticas... ordem de obtenção"
     */
    public void registarAcesso(Thread t, String recurso) {
        // 1. Atualizar contagem
        estatisticasAcessos.merge(t.getName(), 1, Integer::sum);
        
        // 2. Registar ordem
        String evento = String.format("[%d] %s obteve %s", System.currentTimeMillis(), t.getName(), recurso);
        ordemObtencaoRecursos.add(evento);
        
        // Logar o evento
        logger.log("[EVENTO] Acesso garantido: " + t.getName() + " -> " + recurso + 
                   " (Total: " + estatisticasAcessos.get(t.getName()) + ")");
    }

    @Override
    public void run() {
        logger.log("[INFO] Monitor Iniciado.");
        logger.log("[INFO] Algoritmo: Wait-for Graph + DFS para detecao de ciclos");
        
        while (running) {
            try {
                // 1. Deadlock - Usando algoritmo académico
                long[] deadlockedIds = detector.detectar();
                if (deadlockedIds != null && deadlockedIds.length > 0) {
                    String alerta = "[ALERT] DEADLOCK DETETADO! Espera Circular entre " + 
                                   deadlockedIds.length + " threads.";
                    logger.log(alerta);
                    logger.log(detector.obterEstadoGrafo());
                    System.err.println("\n" + alerta);
                    System.err.println(detector.obterEstadoGrafo());
                }

                // 2. Starvation
                synchronized (this) {
                    for (Thread t : threadsVigiadas) {
                        if (t.getState() == State.BLOCKED || t.getState() == State.WAITING) {
                            tempoEspera.putIfAbsent(t, System.currentTimeMillis());
                            long delta = System.currentTimeMillis() - tempoEspera.get(t);
                            if (delta > Config.STARVATION_THRESHOLD_MS) {
                                logger.log("[ALERT] STARVATION: " + t.getName() + " espera ha " + delta + "ms.");
                            }
                        } else {
                            tempoEspera.remove(t);
                        }
                    }
                }
                Thread.sleep(Config.MONITOR_INTERVAL_MS);
            } catch (InterruptedException e) { if(!running) break; }
        }
        logger.log("[INFO] Monitor Parado.");
    }
    
    /**
     * Obtém estatísticas de acessos por thread.
     * Retorna mapa: Nome da Thread → Número de acessos
     */
    public Map<String, Integer> obterEstatisticasAcessos() {
        return new ConcurrentHashMap<>(estatisticasAcessos);
    }
    
    /**
     * Obtém a ordem cronológica de obtenção de exclusividade aos recursos.
     * Útil para análise de execução e debugging.
     */
    public List<String> obterOrdemObtencaoRecursos() {
        synchronized (ordemObtencaoRecursos) {
            return new ArrayList<>(ordemObtencaoRecursos);
        }
    }
    
    /**
     * Imprime relatório de estatísticas no log.
     */
    public void imprimirEstatisticas() {
        logger.log("\n=== ESTATÍSTICAS DE ACESSOS ===");
        if (estatisticasAcessos.isEmpty()) {
            logger.log("(Nenhum acesso registado)");
        } else {
            estatisticasAcessos.forEach((thread, count) -> 
                logger.log(String.format("  %s: %d acessos", thread, count))
            );
        }
        
        logger.log("\n=== ORDEM DE OBTENÇÃO DE RECURSOS ===");
        if (ordemObtencaoRecursos.isEmpty()) {
            logger.log("(Nenhuma obtenção registada)");
        } else {
            synchronized (ordemObtencaoRecursos) {
                int max = Math.min(20, ordemObtencaoRecursos.size()); // Primeiros 20
                for (int i = 0; i < max; i++) {
                    logger.log("  " + ordemObtencaoRecursos.get(i));
                }
                if (ordemObtencaoRecursos.size() > 20) {
                    logger.log("  ... (mais " + (ordemObtencaoRecursos.size() - 20) + " registos)");
                }
            }
        }
        logger.log("==============================\n");
    }
}