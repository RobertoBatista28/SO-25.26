
package simulador;

import monitor.MonitorEBPF;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates how starvation can delay critical services, causing security failures.
 * Scenario: A security monitoring service is starved by lower-priority tasks,
 * allowing security breaches to go undetected.
 */
public class CriticalServiceStarvationDemo {
    private final MonitorEBPF monitor;
    
    // Simulated security events
    static class SecurityEvent {
        String type;
        long timestamp;
        boolean processed = false;
        
        SecurityEvent(String type) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    private final ReentrantLock systemLock = new ReentrantLock(false); // non-fair
    private final ReentrantLock fairSystemLock = new ReentrantLock(true); // fair
    
    private volatile int securityBreaches = 0;
    private volatile int detectedBreaches = 0;
    private volatile long maxSecurityDelay = 0;
    
    public CriticalServiceStarvationDemo(MonitorEBPF monitor) {
        this.monitor = monitor;
    }
    
    /**
     * VULNERABLE: Critical security service is starved by non-critical tasks
     */
    public void runVulnerableScenario() throws InterruptedException {
        System.out.println("== CriticalServiceStarvationDemo: VULNERABLE (security service starved) ==");
        securityBreaches = 0;
        detectedBreaches = 0;
        maxSecurityDelay = 0;
        
        // Simulate security events
        SecurityEvent[] events = new SecurityEvent[10];
        for (int i = 0; i < events.length; i++) {
            events[i] = new SecurityEvent("intrusion-" + i);
        }
        
        // Critical security monitoring service (should be high priority)
        Thread securityMonitor = new Thread(() -> {
            for (SecurityEvent event : events) {
                long requestTime = System.currentTimeMillis();
                monitor.lockRequested("system_monitor", Thread.currentThread());
                systemLock.lock(); // May be starved by other threads
                try {
                    long waitTime = System.currentTimeMillis() - requestTime;
                    maxSecurityDelay = Math.max(maxSecurityDelay, waitTime);
                    
                    monitor.lockAcquired("system_monitor", Thread.currentThread());
                    monitor.recordAccess("security_check", Thread.currentThread(), true);
                    
                    // Check if event is still relevant (not too old)
                    long eventAge = System.currentTimeMillis() - event.timestamp;
                    if (eventAge < 500) { // 500ms threshold
                        event.processed = true;
                        detectedBreaches++;
                        System.out.println("[DETECTED] Security event " + event.type + 
                                         " processed (delay: " + waitTime + "ms)");
                    } else {
                        System.out.println("[MISSED] Security event " + event.type + 
                                         " too old (delay: " + waitTime + "ms) - SECURITY FAILURE!");
                        securityBreaches++;
                    }
                    
                    sleep(10); // Processing time
                } finally {
                    monitor.lockReleased("system_monitor", Thread.currentThread());
                    systemLock.unlock();
                }
            }
        }, "security-monitor");
        
        // Multiple non-critical tasks that hog the system lock
        Thread[] backgroundTasks = new Thread[8];
        for (int i = 0; i < backgroundTasks.length; i++) {
            backgroundTasks[i] = new Thread(() -> {
                for (int j = 0; j < 50; j++) {
                    monitor.lockRequested("system_background", Thread.currentThread());
                    systemLock.lock();
                    try {
                        monitor.lockAcquired("system_background", Thread.currentThread());
                        // Non-critical background work
                        sleep(20);
                    } finally {
                        monitor.lockReleased("system_background", Thread.currentThread());
                        systemLock.unlock();
                    }
                }
            }, "background-" + i);
        }
        
        // Start background tasks first (they will hog the lock)
        for (Thread t : backgroundTasks) t.start();
        sleep(50); // Let them establish dominance
        
        // Now start security monitor (will be starved)
        securityMonitor.start();
        
        securityMonitor.join();
        for (Thread t : backgroundTasks) t.join();
        
        System.out.println("[VULNERABILITY] Detected: " + detectedBreaches + "/" + events.length + 
                         ", Missed (breaches): " + securityBreaches);
        System.out.println("[VULNERABILITY] Max security delay: " + maxSecurityDelay + 
                         "ms (starvation caused security failures)");
    }
    
    /**
     * SECURE: Fair scheduling ensures critical security service is not starved
     */
    public void runSecureScenario() throws InterruptedException {
        System.out.println("== CriticalServiceStarvationDemo: SECURE (fair scheduling) ==");
        securityBreaches = 0;
        detectedBreaches = 0;
        maxSecurityDelay = 0;
        
        SecurityEvent[] events = new SecurityEvent[10];
        for (int i = 0; i < events.length; i++) {
            events[i] = new SecurityEvent("intrusion-secure-" + i);
        }
        
        // Critical security monitoring with fair lock
        Thread securityMonitor = new Thread(() -> {
            for (SecurityEvent event : events) {
                long requestTime = System.currentTimeMillis();
                monitor.lockRequested("system_fair", Thread.currentThread());
                fairSystemLock.lock(); // Fair lock - FIFO ordering
                try {
                    long waitTime = System.currentTimeMillis() - requestTime;
                    maxSecurityDelay = Math.max(maxSecurityDelay, waitTime);
                    
                    monitor.lockAcquired("system_fair", Thread.currentThread());
                    monitor.recordAccess("security_check", Thread.currentThread(), true);
                    
                    long eventAge = System.currentTimeMillis() - event.timestamp;
                    if (eventAge < 500) {
                        event.processed = true;
                        detectedBreaches++;
                        System.out.println("[DETECTED] Security event " + event.type + 
                                         " processed securely (delay: " + waitTime + "ms)");
                    } else {
                        securityBreaches++;
                    }
                    
                    sleep(10);
                } finally {
                    monitor.lockReleased("system_fair", Thread.currentThread());
                    fairSystemLock.unlock();
                }
            }
        }, "security-monitor-secure");
        
        Thread[] backgroundTasks = new Thread[8];
        for (int i = 0; i < backgroundTasks.length; i++) {
            backgroundTasks[i] = new Thread(() -> {
                for (int j = 0; j < 50; j++) {
                    monitor.lockRequested("system_fair_bg", Thread.currentThread());
                    fairSystemLock.lock();
                    try {
                        monitor.lockAcquired("system_fair_bg", Thread.currentThread());
                        sleep(20);
                    } finally {
                        monitor.lockReleased("system_fair_bg", Thread.currentThread());
                        fairSystemLock.unlock();
                    }
                }
            }, "background-secure-" + i);
        }
        
        for (Thread t : backgroundTasks) t.start();
        sleep(50);
        securityMonitor.start();
        
        securityMonitor.join();
        for (Thread t : backgroundTasks) t.join();
        
        System.out.println("[SECURE] Detected: " + detectedBreaches + "/" + events.length + 
                         ", Missed: " + securityBreaches);
        System.out.println("[SECURE] Max security delay: " + maxSecurityDelay + 
                         "ms (fair scheduling prevented starvation)");
    }
    
    /**
     * Authentication service starvation scenario
     */
    public void runAuthenticationStarvationScenario() throws InterruptedException {
        System.out.println("== CriticalServiceStarvationDemo: Authentication service starved ==");
        
        final int[] failedLogins = {0};
        final boolean[] systemCompromised = {false};
        
        ReentrantLock authLock = new ReentrantLock(false);
        
        // Authentication service that should block suspicious activity
        Thread authService = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                long startWait = System.currentTimeMillis();
                monitor.lockRequested("auth_service", Thread.currentThread());
                authLock.lock();
                try {
                    long waited = System.currentTimeMillis() - startWait;
                    monitor.lockAcquired("auth_service", Thread.currentThread());
                    
                    if (waited > 1000) {
                        System.out.println("[CRITICAL] Authentication service delayed by " + waited + 
                                         "ms - attacker may have gained access!");
                        systemCompromised[0] = true;
                    }
                    
                    // Check failed login attempts
                    if (failedLogins[0] > 3) {
                        System.out.println("[AUTH] Blocking suspicious activity (failed logins: " + 
                                         failedLogins[0] + ")");
                        failedLogins[0] = 0;
                    }
                    
                    sleep(50);
                } finally {
                    monitor.lockReleased("auth_service", Thread.currentThread());
                    authLock.unlock();
                }
            }
        }, "auth-service");
        
        // Attacker making many login attempts (should be blocked)
        Thread attacker = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                failedLogins[0]++;
                System.out.println("[ATTACK] Failed login attempt #" + failedLogins[0]);
                sleep(100);
            }
        }, "attacker-login");
        
        // Many non-critical operations hogging the auth lock
        Thread[] hoggers = new Thread[5];
        for (int i = 0; i < hoggers.length; i++) {
            hoggers[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    monitor.lockRequested("auth_hog", Thread.currentThread());
                    authLock.lock();
                    try {
                        monitor.lockAcquired("auth_hog", Thread.currentThread());
                        sleep(30); // Hold lock
                    } finally {
                        monitor.lockReleased("auth_hog", Thread.currentThread());
                        authLock.unlock();
                    }
                }
            }, "hogger-" + i);
        }
        
        for (Thread t : hoggers) t.start();
        sleep(100);
        attacker.start();
        authService.start();
        
        attacker.join();
        authService.join();
        for (Thread t : hoggers) t.join();
        
        if (systemCompromised[0]) {
            System.out.println("[SECURITY BREACH] System compromised due to authentication service starvation!");
        }
        System.out.println("Total failed login attempts: " + failedLogins[0]);
    }
    
    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
