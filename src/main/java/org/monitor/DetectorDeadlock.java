package org.monitor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detector de Deadlocks baseado nos algoritmos de SO-T-05.
 * Implementa manualmente o Wait-for Graph e a deteção de ciclos através de DFS.
 * 
 * 
 * As 4 condições necessárias para deadlock:
 * 1. Exclusão Mútua
 * 2. Retenção e Espera
 * 3. Não Preempção
 * 4. Espera Circular ← DETETADA POR ESTE ALGORITMO
 */
public class DetectorDeadlock {
    
    // Wait-for Graph: Thread → Recursos que detém atualmente
    private final Map<Thread, Set<Object>> recursosDetidos = new ConcurrentHashMap<>();
    
    // Wait-for Graph: Thread → Recursos que aguarda
    private final Map<Thread, Set<Object>> recursosAguardados = new ConcurrentHashMap<>();
    
    // Mapeamento inverso: Recurso → Thread que o detém
    private final Map<Object, Thread> recursoParaThread = new ConcurrentHashMap<>();
    
    public DetectorDeadlock() {}
    
    /**
     * Regista que uma thread está à ESPERA de um recurso (antes de conseguir o lock).
     * Corresponde ao estado "Request" no Modelo Sistémico.
     */
    public synchronized void registarEspera(Thread thread, Object recurso) {
        recursosAguardados.computeIfAbsent(thread, k -> ConcurrentHashMap.newKeySet()).add(recurso);
    }
    
    /**
     * Regista que uma thread OBTEVE um recurso (após lock bem-sucedido).
     * Corresponde ao estado "Use" no Modelo Sistémico.
     */
    public synchronized void registarAlocacao(Thread thread, Object recurso) {
        recursosDetidos.computeIfAbsent(thread, k -> ConcurrentHashMap.newKeySet()).add(recurso);
        recursoParaThread.put(recurso, thread);
        
        // Remove da lista de espera
        Set<Object> aguardados = recursosAguardados.get(thread);
        if (aguardados != null) {
            aguardados.remove(recurso);
        }
    }
    
    /**
     * Regista que uma thread LIBERTOU um recurso.
     * Corresponde ao estado "Release" no Modelo Sistémico.
     */
    public synchronized void registarLibertacao(Thread thread, Object recurso) {
        Set<Object> detidos = recursosDetidos.get(thread);
        if (detidos != null) {
            detidos.remove(recurso);
            if (detidos.isEmpty()) {
                recursosDetidos.remove(thread);
            }
        }
        recursoParaThread.remove(recurso);
    }
    
    /**
     * Implementação do algoritmo de deteção de deadlocks segundo.
     * 
     * ALGORITMO:
     * 1. Construir Wait-for Graph (Thread → Thread)
     * 2. Procurar ciclos usando DFS (Depth-First Search)
     * 3. Ciclo encontrado = Espera Circular = Deadlock
     * 
     * @return Array de IDs das threads em deadlock, ou null se não houver deadlock
     */
    public synchronized long[] detectar() {
        // Construir grafo de espera Thread → Thread
        Map<Thread, Set<Thread>> grafoEspera = construirWaitForGraph();
        
        // Detetar ciclos usando DFS
        Set<Thread> threadsEmDeadlock = new HashSet<>();
        Set<Thread> visitadas = new HashSet<>();
        Set<Thread> pilhaRecursao = new HashSet<>();
        
        for (Thread thread : grafoEspera.keySet()) {
            if (!visitadas.contains(thread)) {
                detetarCicloDFS(thread, grafoEspera, visitadas, pilhaRecursao, threadsEmDeadlock);
            }
        }
        
        if (threadsEmDeadlock.isEmpty()) {
            return null;
        }
        
        // Converter para array de IDs
        return threadsEmDeadlock.stream()
                .mapToLong(Thread::getId)
                .toArray();
    }
    
    /**
     * Constrói o Wait-for Graph conforme.
     * 
     * LÓGICA:
     * Thread P1 espera por Thread P2 se:
     * - P1 aguarda um recurso R
     * - P2 detém o recurso R
     * 
     * Isto cria uma aresta P1 → P2 no grafo.
     */
    private Map<Thread, Set<Thread>> construirWaitForGraph() {
        Map<Thread, Set<Thread>> grafo = new HashMap<>();
        
        for (Map.Entry<Thread, Set<Object>> entry : recursosAguardados.entrySet()) {
            Thread threadEsperando = entry.getKey();
            Set<Object> recursos = entry.getValue();
            
            for (Object recurso : recursos) {
                Thread threadDetentora = recursoParaThread.get(recurso);
                if (threadDetentora != null && threadDetentora != threadEsperando) {
                    // threadEsperando → threadDetentora
                    grafo.computeIfAbsent(threadEsperando, k -> new HashSet<>()).add(threadDetentora);
                }
            }
        }
        
        return grafo;
    }
    
    /**
     * Algoritmo DFS (Depth-First Search) para deteção de ciclos.
     * 
     * TEORIA:
     * "Espera circular: Deve existir um conjunto de processos (P1, P2, ..., Pn)
     * de tal forma que P1 aguarda P2, P2 aguarda P3, ..., Pn aguarda P1"
     * 
     * Um ciclo no grafo representa exatamente esta situação de espera circular.
     */
    private boolean detetarCicloDFS(Thread atual, Map<Thread, Set<Thread>> grafo,
                                     Set<Thread> visitadas, Set<Thread> pilhaRecursao,
                                     Set<Thread> threadsEmDeadlock) {
        
        visitadas.add(atual);
        pilhaRecursao.add(atual);
        
        Set<Thread> vizinhos = grafo.get(atual);
        if (vizinhos != null) {
            for (Thread vizinho : vizinhos) {
                if (!visitadas.contains(vizinho)) {
                    if (detetarCicloDFS(vizinho, grafo, visitadas, pilhaRecursao, threadsEmDeadlock)) {
                        threadsEmDeadlock.add(atual);
                        return true;
                    }
                } else if (pilhaRecursao.contains(vizinho)) {
                    // CICLO ENCONTRADO! (Espera Circular)
                    threadsEmDeadlock.add(atual);
                    threadsEmDeadlock.add(vizinho);
                    return true;
                }
            }
        }
        
        pilhaRecursao.remove(atual);
        return false;
    }
    
    /**
     * Limpa o estado de uma thread (quando termina).
     */
    public synchronized void limparThread(Thread thread) {
        Set<Object> detidos = recursosDetidos.remove(thread);
        if (detidos != null) {
            for (Object recurso : detidos) {
                recursoParaThread.remove(recurso);
            }
        }
        recursosAguardados.remove(thread);
    }
    
    /**
     * Obtém representação textual do Wait-for Graph para diagnóstico.
     * Útil para debugging e demonstração académica.
     */
    public synchronized String obterEstadoGrafo() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Wait-for Graph ===\n");
        
        Map<Thread, Set<Thread>> grafo = construirWaitForGraph();
        if (grafo.isEmpty()) {
            sb.append("(Grafo vazio - sem dependências)\n");
        } else {
            for (Map.Entry<Thread, Set<Thread>> entry : grafo.entrySet()) {
                sb.append(entry.getKey().getName()).append(" aguarda por: ");
                entry.getValue().forEach(t -> sb.append(t.getName()).append(" "));
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
}