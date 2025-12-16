
package monitor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MonitorEBPF: lightweight monitor to log thread/resource events, detect potential race conditions,
 * deadlocks (via wait-for graph cycle detection) and starvation (long waits).
 */
public class MonitorEBPF {
    private final PrintWriter logWriter;
    private final Map<String, List<AccessRecord>> accessLog = new ConcurrentHashMap<>();
    private final Map<Long, Integer> accessesPerThread = new ConcurrentHashMap<>();
    // lockName -> owner thread id (or null)
    private final Map<String, Long> lockOwner = new ConcurrentHashMap<>();
    // threadId -> waiting for lockName (or null)
    private final Map<Long, String> waitingFor = new ConcurrentHashMap<>();
    // threadId -> list of locks acquired order
    private final Map<Long, List<String>> acquisitionOrder = new ConcurrentHashMap<>();
    // wait start times for starvation detection
    private final Map<Long, Long> waitStart = new ConcurrentHashMap<>();

    // thresholds (ms)
    private final long starvationThresholdMs = 3000;
    private final long potentialDeadlockThresholdMs = 2000;

    public MonitorEBPF(String logfile) throws IOException {
        this.logWriter = new PrintWriter(new FileWriter(logfile, false), true);
        log("Monitor started at " + Instant.now());
    }

    public void shutdown() {
        log("Monitor shutting down at " + Instant.now());
        logWriter.close();
    }

    private void log(String s) {
        String line = String.format("[%s] %s", Instant.now(), s);
        System.out.println(line);
        logWriter.println(line);
    }

    public void recordAccess(String resource, Thread t, boolean inCriticalSection) {
        long tid = t.getId();
        accessesPerThread.merge(tid, 1, Integer::sum);
        AccessRecord rec = new AccessRecord(resource, tid, System.currentTimeMillis(), inCriticalSection);
        accessLog.computeIfAbsent(resource, k -> Collections.synchronizedList(new ArrayList<>())).add(rec);
        log(String.format("ACCESS resource=%s thread=%s inCritical=%s", resource, t.getName(), inCriticalSection));
        detectRaceIfAny(resource);
    }

    private void detectRaceIfAny(String resource) {
        List<AccessRecord> list = accessLog.get(resource);
        if (list == null) return;
        // look for accesses by different threads close in time and at least one not protected
        int n = list.size();
        if (n < 2) return;
        AccessRecord a = list.get(n-1);
        AccessRecord b = list.get(n-2);
        long delta = Math.abs(a.timestamp - b.timestamp);
        if (delta < 50 && (!a.inCriticalSection || !b.inCriticalSection) && a.threadId != b.threadId) {
            String msg = String.format("RACE_DETECTED on resource=%s between threads %d and %d (delta=%d ms)", resource, a.threadId, b.threadId, delta);
            log(msg);
        }
    }

    public void lockRequested(String lockName, Thread t) {
        long tid = t.getId();
        waitingFor.put(tid, lockName);
        waitStart.putIfAbsent(tid, System.currentTimeMillis());
        log(String.format("LOCK_REQUESTED lock=%s thread=%s", lockName, t.getName()));
        checkStarvation(tid);
        checkPotentialDeadlock(lockName);
    }

    public void lockAcquired(String lockName, Thread t) {
        long tid = t.getId();
        waitingFor.remove(tid);
        waitStart.remove(tid);
        lockOwner.put(lockName, tid);
        acquisitionOrder.computeIfAbsent(tid, k -> Collections.synchronizedList(new ArrayList<>())).add(lockName);
        log(String.format("LOCK_ACQUIRED lock=%s thread=%s", lockName, t.getName()));
    }

    public void lockReleased(String lockName, Thread t) {
        long tid = t.getId();
        Long owner = lockOwner.get(lockName);
        if (owner != null && owner == tid) {
            lockOwner.remove(lockName);
            log(String.format("LOCK_RELEASED lock=%s thread=%s", lockName, t.getName()));
        } else {
            log(String.format("LOCK_RELEASE_ATTEMPT_NON_OWNER lock=%s thread=%s owner=%s", lockName, t.getName(), owner));
        }
    }

    private void checkStarvation(long tid) {
        Long start = waitStart.get(tid);
        if (start == null) return;
        long waited = System.currentTimeMillis() - start;
        if (waited > starvationThresholdMs) {
            log(String.format("STARVATION_PATTERN thread=%d waited=%d ms", tid, waited));
        }
    }

    private void checkPotentialDeadlock(String lockName) {
        // If a thread is waiting for a lock held by another thread that is waiting for a lock held by the first, etc.
        // We'll check cycles when wait time exceeds threshold.
        // quick check: if lock owner exists, and owner is waiting for some lock held by this requester, possible cycle.
        Long owner = lockOwner.get(lockName);
        if (owner == null) return;
        // find any thread waiting for a lock that owner holds? This simplistic check logs potential deadlock if mutual waiting is observed.
        for (Map.Entry<Long, String> e : waitingFor.entrySet()) {
            Long waiter = e.getKey();
            String waitingLock = e.getValue();
            if (waitingLock == null) continue;
            Long waitingLockOwner = lockOwner.get(waitingLock);
            if (waitingLockOwner != null && waitingLockOwner.equals(owner) && lockOwner.get(lockName).equals(waitingLockOwner)) {
                // owner waits for a lock owned by waiter? simplified detection via timestamps
                log(String.format("POTENTIAL_DEADLOCK involving threads %d and %d locks %s and %s", owner, waiter, lockName, waitingLock));
            }
        }
        // Additionally, run full cycle detection periodically
        detectDeadlockCycles();
    }

    private void detectDeadlockCycles() {
        // Build wait-for graph: waiting thread -> owner thread
        Map<Long, Long> graph = new HashMap<>();
        for (Map.Entry<Long, String> e : waitingFor.entrySet()) {
            Long waiter = e.getKey();
            String lock = e.getValue();
            if (lock == null) continue;
            Long owner = lockOwner.get(lock);
            if (owner != null) {
                graph.put(waiter, owner);
            }
        }
        // detect cycle
        Set<Long> visited = new HashSet<>();
        Set<Long> stack = new HashSet<>();
        for (Long node : graph.keySet()) {
            if (dfsCycle(node, graph, visited, stack)) {
                log("DEADLOCK_DETECTED among threads in wait-for graph: " + graph);
                // log and break
                return;
            }
        }
    }

    private boolean dfsCycle(Long node, Map<Long, Long> graph, Set<Long> visited, Set<Long> stack) {
        if (stack.contains(node)) return true;
        if (visited.contains(node)) return false;
        visited.add(node);
        stack.add(node);
        Long neighbor = graph.get(node);
        if (neighbor != null && dfsCycle(neighbor, graph, visited, stack)) return true;
        stack.remove(node);
        return false;
    }

    public Map<Long, Integer> getAccessesPerThread() {
        return Collections.unmodifiableMap(accessesPerThread);
    }

    public Map<Long, List<String>> getAcquisitionOrder() {
        return acquisitionOrder;
    }

    // AccessRecord for internal access logging
    private static class AccessRecord {
        String resource;
        long threadId;
        long timestamp;
        boolean inCriticalSection;

        AccessRecord(String resource, long threadId, long timestamp, boolean inCriticalSection) {
            this.resource = resource;
            this.threadId = threadId;
            this.timestamp = timestamp;
            this.inCriticalSection = inCriticalSection;
        }
    }
}
