
package simulador;

import monitor.MonitorEBPF;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates a race condition that can lead to privilege escalation.
 * Scenario: A user authentication system where race conditions allow bypassing privilege checks.
 */
public class PrivilegeEscalationDemo {
    private final MonitorEBPF monitor;
    
    // Simulated user database
    static class User {
        String username;
        boolean isAdmin = false;
        boolean isAuthenticated = false;
        
        User(String username) {
            this.username = username;
        }
    }
    
    private User currentUser = new User("guest");
    private final ReentrantLock authLock = new ReentrantLock();
    
    public PrivilegeEscalationDemo(MonitorEBPF monitor) {
        this.monitor = monitor;
    }
    
    /**
     * VULNERABLE: Race condition allows privilege escalation
     * The check and grant operations are not atomic.
     */
    public void runVulnerableScenario() throws InterruptedException {
        System.out.println("== PrivilegeEscalationDemo: VULNERABLE (race condition allows privilege bypass) ==");
        
        // Attacker thread tries to exploit race condition
        Thread attacker = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                monitor.recordAccess("privilege_check", Thread.currentThread(), false);
                // Check if user can be admin (vulnerable - no synchronization)
                if (!currentUser.isAdmin) {
                    monitor.recordAccess("privilege_data", Thread.currentThread(), false);
                    try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                    // Race window here - another thread might change state
                    currentUser.isAdmin = true;
                    System.out.println("[SECURITY BREACH] " + Thread.currentThread().getName() + 
                                     " escalated privileges to admin!");
                }
            }
        }, "attacker");
        
        // Legitimate admin thread
        Thread admin = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                monitor.recordAccess("privilege_check", Thread.currentThread(), false);
                if (currentUser.isAdmin) {
                    monitor.recordAccess("admin_operation", Thread.currentThread(), false);
                    // Perform admin operation
                    try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                    currentUser.isAdmin = false; // Reset
                }
            }
        }, "admin-reset");
        
        attacker.start();
        admin.start();
        attacker.join();
        admin.join();
        
        System.out.println("Final user state: isAdmin=" + currentUser.isAdmin + 
                         " (should be false if secure)");
    }
    
    /**
     * SECURE: Proper synchronization prevents privilege escalation
     */
    public void runSecureScenario() throws InterruptedException {
        System.out.println("== PrivilegeEscalationDemo: SECURE (synchronized privilege checks) ==");
        currentUser = new User("guest"); // Reset
        
        Thread attacker = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                monitor.lockRequested("privilege_lock", Thread.currentThread());
                authLock.lock();
                try {
                    monitor.lockAcquired("privilege_lock", Thread.currentThread());
                    monitor.recordAccess("privilege_check", Thread.currentThread(), true);
                    
                    if (!currentUser.isAdmin) {
                        monitor.recordAccess("privilege_data", Thread.currentThread(), true);
                        // Atomic check and set - no race condition
                        currentUser.isAdmin = true;
                        System.out.println("[BLOCKED] " + Thread.currentThread().getName() + 
                                         " attempted privilege escalation (synchronized)");
                        currentUser.isAdmin = false; // Immediately revoked
                    }
                } finally {
                    monitor.lockReleased("privilege_lock", Thread.currentThread());
                    authLock.unlock();
                }
            }
        }, "attacker-sync");
        
        Thread admin = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                monitor.lockRequested("privilege_lock", Thread.currentThread());
                authLock.lock();
                try {
                    monitor.lockAcquired("privilege_lock", Thread.currentThread());
                    monitor.recordAccess("privilege_check", Thread.currentThread(), true);
                    
                    if (currentUser.isAdmin) {
                        monitor.recordAccess("admin_operation", Thread.currentThread(), true);
                        currentUser.isAdmin = false;
                    }
                } finally {
                    monitor.lockReleased("privilege_lock", Thread.currentThread());
                    authLock.unlock();
                }
            }
        }, "admin-sync");
        
        attacker.start();
        admin.start();
        attacker.join();
        admin.join();
        
        System.out.println("Secure scenario completed - no unauthorized privilege escalation");
    }
    
    /**
     * Data corruption scenario: race condition corrupts financial transaction
     */
    public void runDataCorruptionScenario() throws InterruptedException {
        System.out.println("== PrivilegeEscalationDemo: DATA CORRUPTION via race condition ==");
        
        final int[] balance = {1000}; // Shared account balance
        
        Thread[] transactions = new Thread[5];
        for (int i = 0; i < transactions.length; i++) {
            final int amount = (i + 1) * 100;
            transactions[i] = new Thread(() -> {
                monitor.recordAccess("account_balance", Thread.currentThread(), false);
                int current = balance[0];
                try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                balance[0] = current - amount; // Race condition - lost updates
                System.out.println(Thread.currentThread().getName() + " withdrew " + amount + 
                                 ", balance=" + balance[0]);
            }, "transaction-" + i);
        }
        
        for (Thread t : transactions) t.start();
        for (Thread t : transactions) t.join();
        
        int expected = 1000 - (100 + 200 + 300 + 400 + 500);
        System.out.println("[DATA CORRUPTION] Expected balance: " + expected + 
                         ", Actual: " + balance[0] + " (data lost due to race condition)");
    }
}
