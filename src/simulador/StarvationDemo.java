
package simulador;

import monitor.MonitorEBPF;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates starvation: a low-priority thread has trouble acquiring a busy lock.
 */
public class StarvationDemo {
    private final ReentrantLock busyLock = new ReentrantLock(false); // non-fair may cause starvation
    private final MonitorEBPF monitor;

    public StarvationDemo(MonitorEBPF monitor) {
        this.monitor = monitor;
    }

    public void runStarvationScenario() throws InterruptedException {
        System.out.println("== StarvationDemo: creating potential starvation ==");
        // High frequency workers that hog the lock
        Runnable hogger = () -> {
            for (int i = 0; i < 200; i++) {
                monitor.lockRequested("BUSY", Thread.currentThread());
                busyLock.lock();
                try {
                    monitor.lockAcquired("BUSY", Thread.currentThread());
                    // short critical work
                    try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                } finally {
                    monitor.lockReleased("BUSY", Thread.currentThread());
                    busyLock.unlock();
                }
            }
        };

        Thread lowPriority = new Thread(() -> {
            // lower priority simulation: tries less often but may be starved
            for (int i = 0; i < 10; i++) {
                monitor.lockRequested("BUSY", Thread.currentThread());
                long startWait = System.currentTimeMillis();
                busyLock.lock();
                try {
                    monitor.lockAcquired("BUSY", Thread.currentThread());
                    long waited = System.currentTimeMillis() - startWait;
                    if (waited > 50) {
                        monitor.lockReleased("BUSY", Thread.currentThread());
                        // a record of starvation-like long waits
                        monitor.lockRequested("BUSY", Thread.currentThread());
                    }
                    try { Thread.sleep(5); } catch (InterruptedException ignored) {}
                } finally {
                    monitor.lockReleased("BUSY", Thread.currentThread());
                    busyLock.unlock();
                }
            }
        }, "low-priority");

        Thread[] hogs = new Thread[4];
        for (int i = 0; i < hogs.length; i++) {
            hogs[i] = new Thread(hogger, "hog-" + i);
            hogs[i].start();
        }
        lowPriority.start();

        for (Thread t : hogs) t.join();
        lowPriority.join();
        System.out.println("Starvation scenario finished (check monitor.log for STARVATION entries).");
    }

    // Corrected version using fair lock to prevent starvation
    public void runCorrected() throws InterruptedException {
        System.out.println("== StarvationDemo: corrected (fair lock) ==");
        ReentrantLock fairLock = new ReentrantLock(true);
        Thread hog = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                monitor.lockRequested("FAIR", Thread.currentThread());
                fairLock.lock();
                try {
                    monitor.lockAcquired("FAIR", Thread.currentThread());
                    try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                } finally {
                    monitor.lockReleased("FAIR", Thread.currentThread());
                    fairLock.unlock();
                }
            }
        }, "fair-hog");

        Thread low = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                monitor.lockRequested("FAIR", Thread.currentThread());
                fairLock.lock();
                try {
                    monitor.lockAcquired("FAIR", Thread.currentThread());
                    try { Thread.sleep(5); } catch (InterruptedException ignored) {}
                } finally {
                    monitor.lockReleased("FAIR", Thread.currentThread());
                    fairLock.unlock();
                }
            }
        }, "fair-low");

        hog.start();
        low.start();
        hog.join();
        low.join();
    }
}
