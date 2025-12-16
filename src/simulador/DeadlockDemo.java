
package simulador;

import monitor.MonitorEBPF;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates a deliberate deadlock and a corrected version.
 */
public class DeadlockDemo {
    private final ReentrantLock lockA = new ReentrantLock();
    private final ReentrantLock lockB = new ReentrantLock();
    private final MonitorEBPF monitor;

    public DeadlockDemo(MonitorEBPF monitor) {
        this.monitor = monitor;
    }

    // Create deadlock by acquiring locks in different orders
    public void runDeadlockScenario() throws InterruptedException {
        System.out.println("== DeadlockDemo: creating deadlock ==");
        Thread t1 = new Thread(() -> {
            monitor.lockRequested("A", Thread.currentThread());
            lockA.lock();
            try {
                monitor.lockAcquired("A", Thread.currentThread());
                sleep(50);
                monitor.lockRequested("B", Thread.currentThread());
                // attempt to acquire lockB
                lockB.lock();
                try {
                    monitor.lockAcquired("B", Thread.currentThread());
                } finally {
                    monitor.lockReleased("B", Thread.currentThread());
                    lockB.unlock();
                }
            } finally {
                monitor.lockReleased("A", Thread.currentThread());
                lockA.unlock();
            }
        }, "t-dead-1");

        Thread t2 = new Thread(() -> {
            monitor.lockRequested("B", Thread.currentThread());
            lockB.lock();
            try {
                monitor.lockAcquired("B", Thread.currentThread());
                sleep(50);
                monitor.lockRequested("A", Thread.currentThread());
                lockA.lock();
                try {
                    monitor.lockAcquired("A", Thread.currentThread());
                } finally {
                    monitor.lockReleased("A", Thread.currentThread());
                    lockA.unlock();
                }
            } finally {
                monitor.lockReleased("B", Thread.currentThread());
                lockB.unlock();
            }
        }, "t-dead-2");

        t1.start();
        t2.start();

        // wait some time to allow deadlock to arise and be detected
        t1.join(2000);
        t2.join(2000);
        System.out.println("If threads are stuck, a deadlock occurred (check monitor.log).");
    }

    // Corrected version: enforce lock ordering
    public void runCorrected(int runs) throws InterruptedException {
        System.out.println("== DeadlockDemo: corrected (global lock ordering) ==");
        Thread[] threads = new Thread[runs];
        for (int i = 0; i < runs; i++) {
            threads[i] = new Thread(() -> {
                // always acquire A then B
                monitor.lockRequested("A", Thread.currentThread());
                lockA.lock();
                try {
                    monitor.lockAcquired("A", Thread.currentThread());
                    monitor.lockRequested("B", Thread.currentThread());
                    lockB.lock();
                    try {
                        monitor.lockAcquired("B", Thread.currentThread());
                        // do work
                        sleep(10);
                    } finally {
                        monitor.lockReleased("B", Thread.currentThread());
                        lockB.unlock();
                    }
                } finally {
                    monitor.lockReleased("A", Thread.currentThread());
                    lockA.unlock();
                }
            }, "t-corr-" + i);
            threads[i].start();
        }
        for (Thread t : threads) t.join();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
