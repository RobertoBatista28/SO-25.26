
package simulador;

import monitor.MonitorEBPF;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates how deadlocks can be exploited as Denial of Service (DoS) attacks.
 * Scenario: An attacker deliberately creates deadlocks to block critical services.
 */
public class DenialOfServiceDemo {
    private final MonitorEBPF monitor;
    
    // Critical system resources
    private final ReentrantLock databaseLock = new ReentrantLock();
    private final ReentrantLock networkLock = new ReentrantLock();
    
    private volatile int successfulRequests = 0;
    private volatile int failedRequests = 0;
    
    public DenialOfServiceDemo(MonitorEBPF monitor) {
        this.monitor = monitor;
    }
    
    /**
     * DoS Attack: Attacker creates deliberate deadlock to block legitimate services
     */
    public void runDoSAttack() throws InterruptedException {
        System.out.println("== DenialOfServiceDemo: DoS via deliberate deadlock ==");
        successfulRequests = 0;
        failedRequests = 0;
        
        // Attacker thread 1: Acquires database then tries network
        Thread attacker1 = new Thread(() -> {
            monitor.lockRequested("database", Thread.currentThread());
            databaseLock.lock();
            try {
                monitor.lockAcquired("database", Thread.currentThread());
                System.out.println("[ATTACK] " + Thread.currentThread().getName() + 
                                 " locked database resource");
                sleep(100); // Hold the lock
                
                monitor.lockRequested("network", Thread.currentThread());
                networkLock.lock();
                try {
                    monitor.lockAcquired("network", Thread.currentThread());
                } finally {
                    monitor.lockReleased("network", Thread.currentThread());
                    networkLock.unlock();
                }
            } finally {
                monitor.lockReleased("database", Thread.currentThread());
                databaseLock.unlock();
            }
        }, "attacker-1");
        
        // Attacker thread 2: Acquires network then tries database (opposite order = deadlock)
        Thread attacker2 = new Thread(() -> {
            monitor.lockRequested("network", Thread.currentThread());
            networkLock.lock();
            try {
                monitor.lockAcquired("network", Thread.currentThread());
                System.out.println("[ATTACK] " + Thread.currentThread().getName() + 
                                 " locked network resource");
                sleep(100); // Hold the lock
                
                monitor.lockRequested("database", Thread.currentThread());
                databaseLock.lock();
                try {
                    monitor.lockAcquired("database", Thread.currentThread());
                } finally {
                    monitor.lockReleased("database", Thread.currentThread());
                    databaseLock.unlock();
                }
            } finally {
                monitor.lockReleased("network", Thread.currentThread());
                networkLock.unlock();
            }
        }, "attacker-2");
        
        // Legitimate service threads that need these resources
        Thread[] services = new Thread[5];
        for (int i = 0; i < services.length; i++) {
            services[i] = new Thread(() -> {
                for (int j = 0; j < 3; j++) {
                    try {
                        if (processRequest()) {
                            successfulRequests++;
                        } else {
                            failedRequests++;
                            System.out.println("[DoS IMPACT] " + Thread.currentThread().getName() + 
                                             " request failed - service blocked!");
                        }
                    } catch (Exception e) {
                        failedRequests++;
                    }
                    sleep(50);
                }
            }, "service-" + i);
        }
        
        // Start attack
        attacker1.start();
        sleep(10);
        attacker2.start();
        
        // Start legitimate services
        sleep(50);
        for (Thread t : services) t.start();
        
        // Wait with timeout (deadlock will prevent completion)
        attacker1.join(2000);
        attacker2.join(2000);
        for (Thread t : services) t.join(1000);
        
        System.out.println("[DoS RESULT] Successful requests: " + successfulRequests + 
                         ", Failed requests: " + failedRequests);
        System.out.println("[DoS RESULT] Service availability compromised due to deadlock attack!");
    }
    
    /**
     * Mitigated version: Timeout-based resource acquisition prevents permanent DoS
     */
    public void runMitigatedScenario() throws InterruptedException {
        System.out.println("== DenialOfServiceDemo: MITIGATED (timeout-based locks) ==");
        successfulRequests = 0;
        failedRequests = 0;
        
        Thread[] services = new Thread[5];
        for (int i = 0; i < services.length; i++) {
            services[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    boolean acquired1 = false, acquired2 = false;
                    try {
                        monitor.lockRequested("database", Thread.currentThread());
                        // Try with timeout instead of blocking forever
                        acquired1 = databaseLock.tryLock();
                        if (acquired1) {
                            monitor.lockAcquired("database", Thread.currentThread());
                            
                            monitor.lockRequested("network", Thread.currentThread());
                            acquired2 = networkLock.tryLock();
                            if (acquired2) {
                                monitor.lockAcquired("network", Thread.currentThread());
                                // Process request
                                sleep(10);
                                successfulRequests++;
                            } else {
                                failedRequests++;
                                System.out.println("[TIMEOUT] " + Thread.currentThread().getName() + 
                                                 " couldn't acquire network - retry later");
                            }
                        } else {
                            failedRequests++;
                        }
                    } finally {
                        if (acquired2) {
                            monitor.lockReleased("network", Thread.currentThread());
                            networkLock.unlock();
                        }
                        if (acquired1) {
                            monitor.lockReleased("database", Thread.currentThread());
                            databaseLock.unlock();
                        }
                    }
                    sleep(20);
                }
            }, "service-mitigated-" + i);
        }
        
        for (Thread t : services) t.start();
        for (Thread t : services) t.join();
        
        System.out.println("[MITIGATED] Successful requests: " + successfulRequests + 
                         ", Failed requests: " + failedRequests);
        System.out.println("[MITIGATED] Service remained available despite contention");
    }
    
    /**
     * Resource exhaustion DoS: Multiple threads hold locks indefinitely
     */
    public void runResourceExhaustionDoS() throws InterruptedException {
        System.out.println("== DenialOfServiceDemo: Resource exhaustion attack ==");
        
        ReentrantLock[] resourcePool = new ReentrantLock[3];
        for (int i = 0; i < resourcePool.length; i++) {
            resourcePool[i] = new ReentrantLock();
        }
        
        // Attackers grab all resources and hold them
        Thread[] attackers = new Thread[3];
        for (int i = 0; i < attackers.length; i++) {
            final int idx = i;
            attackers[i] = new Thread(() -> {
                monitor.lockRequested("resource-" + idx, Thread.currentThread());
                resourcePool[idx].lock();
                try {
                    monitor.lockAcquired("resource-" + idx, Thread.currentThread());
                    System.out.println("[ATTACK] " + Thread.currentThread().getName() + 
                                     " holding resource-" + idx + " indefinitely");
                    sleep(3000); // Hold for long time
                } finally {
                    monitor.lockReleased("resource-" + idx, Thread.currentThread());
                    resourcePool[idx].unlock();
                }
            }, "resource-hog-" + i);
        }
        
        for (Thread t : attackers) t.start();
        sleep(100);
        
        // Legitimate users can't get resources
        Thread legitimateUser = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                monitor.lockRequested("resource-" + i, Thread.currentThread());
                boolean acquired = resourcePool[i].tryLock();
                if (acquired) {
                    try {
                        monitor.lockAcquired("resource-" + i, Thread.currentThread());
                        System.out.println("[SUCCESS] Got resource-" + i);
                    } finally {
                        monitor.lockReleased("resource-" + i, Thread.currentThread());
                        resourcePool[i].unlock();
                    }
                } else {
                    System.out.println("[BLOCKED] Resource-" + i + 
                                     " unavailable - DoS in effect!");
                }
            }
        }, "legitimate-user");
        
        legitimateUser.start();
        legitimateUser.join();
        
        for (Thread t : attackers) t.join();
        System.out.println("Resource exhaustion attack completed");
    }
    
    private boolean processRequest() {
        monitor.lockRequested("database", Thread.currentThread());
        boolean acquired = databaseLock.tryLock();
        if (!acquired) return false;
        
        try {
            monitor.lockAcquired("database", Thread.currentThread());
            sleep(10);
            return true;
        } finally {
            monitor.lockReleased("database", Thread.currentThread());
            databaseLock.unlock();
        }
    }
    
    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
