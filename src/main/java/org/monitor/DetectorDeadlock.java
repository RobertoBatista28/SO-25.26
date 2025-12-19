package org.monitor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detector de Deadlocks baseado nos algoritmos de SO-T-05.
 * Implementa manualmente o Wait-for Graph e a deteção de ciclos através de DFS.
 */
public class DetectorDeadlock {
    
    // Wait-for Graph: Thread -> Recursos que detém atualmente
    private final Map<Thread, Set<Object>> recursosDetidos = new ConcurrentHashMap<>();
    
    // Wait-for Graph: Thread -> Recursos que aguarda
    private final Map<Thread, Set<Object>> recursosAguardados = new ConcurrentHashMap<>();
    
    // Mapeamento inverso: Recurso -> Thread que o detém
    private final Map<Object, Thread> recursoParaThread = new ConcurrentHashMap<>();
    
    public DetectorDeadlock() {}
    
    /**
     * Regista que uma thread está à ESPERA de um recurso (Request edge).
     */
    public synchronized void registarEspera(Thread thread, Object recurso) {
        if (recurso == null) return;
        recursosAguardados.computeIfAbsent(thread, k -> ConcurrentHashMap.newKeySet()).add(recurso);
    }
    
    /**
     * Regista que uma thread OBTEVE o recurso (Allocation edge).
     */
    public synchronized void registarAlocacao(Thread thread, Object recurso) {
        if (recurso == null) return;
        recursosDetidos.computeIfAbsent(thread, k -> ConcurrentHashMap.newKeySet()).add(recurso);
        recursoParaThread.put(recurso, thread);
        
        // Remove da lista de espera pois já obteve
        Set<Object> aguardados = recursosAguardados.get(thread);
        if (aguardados != null) {
            aguardados.remove(recurso);
        }
    }
    
    /**
     * Regista a libertação de um recurso específico.
     */
    public synchronized void registarLibertacao(Thread thread, Object recurso) {
        if (recurso == null) return; // Proteção contra NullPointerException

        Set<Object> detidos = recursosDetidos.get(thread);
        if (detidos != null) {
            detidos.remove(recurso);
            if (detidos.isEmpty()) recursosDetidos.remove(thread);
        }
        recursoParaThread.remove(recurso);
    }
    
    /**
     * Limpa todo o estado de uma thread (quando termina ou é untracked).     */
    public synchronized void limparThread(Thread thread) {
        // 1. Remover todos os recursos detidos por esta thread do mapa inverso
        Set<Object> detidos = recursosDetidos.remove(thread); // Remove e retorna o set
        if (detidos != null) {
            for (Object recurso : detidos) {
                if (recurso != null) {
                    recursoParaThread.remove(recurso);
                }
            }
        }
        
        // 2. Remover da lista de espera
        recursosAguardados.remove(thread);
    }
    
    /**
     * Algoritmo de deteção de ciclos (DFS) para detetar Espera Circular.
     */
    public synchronized long[] detectar() {
        Map<Thread, Set<Thread>> grafoEspera = construirWaitForGraph();
        Set<Thread> threadsEmDeadlock = new HashSet<>();
        Set<Thread> visitadas = new HashSet<>();
        Set<Thread> pilhaRecursao = new HashSet<>();
        
        for (Thread thread : grafoEspera.keySet()) {
            if (!visitadas.contains(thread)) {
                detetarCicloDFS(thread, grafoEspera, visitadas, pilhaRecursao, threadsEmDeadlock);
            }
        }
        
        if (threadsEmDeadlock.isEmpty()) return null;
        return threadsEmDeadlock.stream().mapToLong(Thread::getId).toArray();
    }
    
    private Map<Thread, Set<Thread>> construirWaitForGraph() {
        Map<Thread, Set<Thread>> grafo = new HashMap<>();
        for (Map.Entry<Thread, Set<Object>> entry : recursosAguardados.entrySet()) {
            Thread threadEsperando = entry.getKey();
            for (Object recurso : entry.getValue()) {
                Thread threadDetentora = recursoParaThread.get(recurso);
                if (threadDetentora != null && threadDetentora != threadEsperando) {
                    grafo.computeIfAbsent(threadEsperando, k -> new HashSet<>()).add(threadDetentora);
                }
            }
        }
        return grafo;
    }
    
    private boolean detetarCicloDFS(Thread atual, Map<Thread, Set<Thread>> grafo, Set<Thread> visitadas, Set<Thread> pilhaRecursao, Set<Thread> threadsEmDeadlock) {
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
                    threadsEmDeadlock.add(atual);
                    threadsEmDeadlock.add(vizinho);
                    return true;
                }
            }
        }
        pilhaRecursao.remove(atual);
        return false;
    }

    public synchronized String obterEstadoGrafo() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Wait-for Graph Dump ---\n");
        Map<Thread, Set<Thread>> grafo = construirWaitForGraph();
        if(grafo.isEmpty()) sb.append("Sem dependências ativas.\n");
        grafo.forEach((t, deps) -> {
            sb.append(t.getName()).append(" aguarda por: ");
            deps.forEach(d -> sb.append(d.getName()).append(" "));
            sb.append("\n");
        });
        return sb.toString();
    }
}