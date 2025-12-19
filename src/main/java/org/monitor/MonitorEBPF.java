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
    private final Map<String, List<Long>> temposEsperaIndividuais = new ConcurrentHashMap<>(); // Thread → Lista de tempos
    private final Map<Thread, Long> inicioEsperaAtual = new ConcurrentHashMap<>(); // Thread → Timestamp início
    private final Map<String, Thread> recursoEmUso = new ConcurrentHashMap<>(); // Recurso → Thread atual (para detetar race conditions)
    private final Map<String, Long> ultimoAcessoRecurso = new ConcurrentHashMap<>(); // Recurso → Timestamp

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
        threadsVigiadas.add(t); 
    }
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

    public void shutdown() { 
        running = false; this.interrupt(); 
    }

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
        
        // 3. Calcular tempo de espera se estava a aguardar
        Long inicio = inicioEsperaAtual.remove(t);
        if (inicio != null) {
            long tempoEspera = System.currentTimeMillis() - inicio;
            temposEsperaIndividuais.computeIfAbsent(t.getName(), k -> Collections.synchronizedList(new ArrayList<>()))
                                    .add(tempoEspera);
            logger.log("[TIMING] " + t.getName() + " aguardou " + tempoEspera + "ms por " + recurso);
        }
        
        // Logar o evento
        logger.log("[EVENTO] Acesso garantido: " + t.getName() + " -> " + recurso + 
                   " (Total: " + estatisticasAcessos.get(t.getName()) + ")");
    }
    
    /**
     * Regista o início de espera por um recurso (chamado ANTES de tentar lock).
     */
    public void registarInicioEspera(Thread t, String recurso) {
        inicioEsperaAtual.put(t, System.currentTimeMillis());
    }
    
    /**
     * Regista acesso a recurso partilhado SEM sincronização (para deteção de race conditions).
     * Alerta se múltiplas threads acedem ao mesmo recurso num intervalo curto.
     */
    public void registarAcessoNaoSincronizado(Thread t, String recurso) {
        long agora = System.currentTimeMillis();
        Thread threadAnterior = recursoEmUso.get(recurso);
        Long tempoAnterior = ultimoAcessoRecurso.get(recurso);
        
        // Se outra thread acedeu recentemente (< 50ms) = POSSÍVEL RACE CONDITION
        if (threadAnterior != null && threadAnterior != t && 
            tempoAnterior != null && (agora - tempoAnterior) < 50) {
            String alerta = String.format("[ALERT] POSSÍVEL RACE CONDITION: %s e %s acederam '%s' em %dms",
                                         threadAnterior.getName(), t.getName(), recurso, (agora - tempoAnterior));
            logger.log(alerta);
            System.err.println("\n⚠️ " + alerta);
        }
        
        recursoEmUso.put(recurso, t);
        ultimoAcessoRecurso.put(recurso, agora);
    }

    @Override
    public void run() {
        logger.log("[INFO] Monitor Iniciado.");
        
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
            } catch (InterruptedException e) {
                if(!running)
                    break; 
                }
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
     * Obtém estatísticas de tempos de espera por thread.
     * Retorna mapa: Nome da Thread → Lista de tempos de espera (ms)
     */
    public Map<String, List<Long>> obterTemposEspera() {
        Map<String, List<Long>> copia = new ConcurrentHashMap<>();
        temposEsperaIndividuais.forEach((thread, tempos) -> {
            synchronized (tempos) {
                copia.put(thread, new ArrayList<>(tempos));
            }
        });
        return copia;
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
        
        logger.log("\n=== TEMPOS DE ESPERA POR SECÇÃO CRÍTICA ===");
        if (temposEsperaIndividuais.isEmpty()) {
            logger.log("(Nenhum tempo registado)");
        } else {
            temposEsperaIndividuais.forEach((thread, tempos) -> {
                synchronized (tempos) {
                    if (!tempos.isEmpty()) {
                        long soma = tempos.stream().mapToLong(Long::longValue).sum();
                        long media = soma / tempos.size();
                        long max = tempos.stream().mapToLong(Long::longValue).max().orElse(0);
                        long min = tempos.stream().mapToLong(Long::longValue).min().orElse(0);
                        logger.log(String.format("  %s: %d acessos | Média: %dms | Min: %dms | Max: %dms", 
                                                  thread, tempos.size(), media, min, max));
                    }
                }
            });
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