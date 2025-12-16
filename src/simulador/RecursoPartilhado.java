
package simulador;

import monitor.MonitorEBPF;

import java.util.concurrent.locks.ReentrantLock;

/**
 * RecursoPartilhado - simulates a shared integer resource with synchronized and unsynchronized access methods.
 */
public class RecursoPartilhado {
    private int valor = 0;
    private final String name;
    private final ReentrantLock lock = new ReentrantLock(true); // fair lock for corrected version
    private final MonitorEBPF monitor;

    public RecursoPartilhado(String name, MonitorEBPF monitor) {
        this.name = name;
        this.monitor = monitor;
    }

    // UNSYNCHRONIZED increment - used to demonstrate race condition
    public void incrementUnsync() {
        // no lock, but we record access as not protected
        monitor.recordAccess(name, Thread.currentThread(), false);
        int temp = valor;
        // simulate work
        try { Thread.sleep(1); } catch (InterruptedException ignored) {}
        valor = temp + 1;
    }

    // SYNCHRONIZED increment using lock
    public void incrementWithLock() {
        monitor.lockRequested(name + "_lock", Thread.currentThread());
        lock.lock();
        try {
            monitor.lockAcquired(name + "_lock", Thread.currentThread());
            monitor.recordAccess(name, Thread.currentThread(), true);
            int temp = valor;
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
            valor = temp + 1;
        } finally {
            monitor.lockReleased(name + "_lock", Thread.currentThread());
            lock.unlock();
        }
    }

    public int getValor() {
        return valor;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public String getName() {
        return name;
    }
}
