package org.monitor;
import java.util.ArrayList;
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
    public synchronized void untrack(Thread t) { threadsVigiadas.remove(t); tempoEspera.remove(t); }

    public void shutdown() { running = false; this.interrupt(); }

    @Override
    public void run() {
        logger.log("[INFO] Monitor Iniciado.");
        while (running) {
            try {
                // 1. Deadlock
                long[] deadlockedIds = detector.detectar();
                if (deadlockedIds != null && deadlockedIds.length > 0) {
                    logger.log("[ALERT] DEADLOCK DETETADO! " + deadlockedIds.length + " threads presas.");
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
}