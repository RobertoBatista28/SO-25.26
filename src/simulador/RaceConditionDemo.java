
package simulador;

import monitor.MonitorEBPF;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates race condition and its corrected version.
 */
public class RaceConditionDemo {
    private final RecursoPartilhado recurso;
    private final MonitorEBPF monitor;

    public RaceConditionDemo(RecursoPartilhado recurso, MonitorEBPF monitor) {
        this.recurso = recurso;
        this.monitor = monitor;
    }

    public void runUnsynchronized(int nThreads, int incrementsPerThread) throws InterruptedException {
        System.out.println("== RaceConditionDemo: unsynchronized ==");
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < nThreads; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    recurso.incrementUnsync();
                }
            }, "unsync-" + i);
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();
        System.out.println("Expected value: " + (nThreads * incrementsPerThread) + " Actual: " + recurso.getValor());
    }

    public void runSynchronized(int nThreads, int incrementsPerThread) throws InterruptedException {
        System.out.println("== RaceConditionDemo: synchronized (with locks) ==");
        List<Thread> threads = new ArrayList<>();
        // reset value by reflection (simple approach not exposing setter)
        // create a fresh resource for correctness in Main
        for (int i = 0; i < nThreads; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    recurso.incrementWithLock();
                }
            }, "sync-" + i);
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();
        System.out.println("Expected value: " + (nThreads * incrementsPerThread) + " Actual: " + recurso.getValor());
    }
}
