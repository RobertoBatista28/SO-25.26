package org.monitor;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class DetectorDeadlock {
    private final ThreadMXBean threadMXBean;

    public DetectorDeadlock() {
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    public long[] detectar() {
        return threadMXBean.findDeadlockedThreads();
    }
}