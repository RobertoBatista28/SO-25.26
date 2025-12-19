package org.app;

public class Config {
    // Caminho para guardar os logs (tem de ser public static final)
    public static final String LOG_FILE = "src/main/java/logs/monitor_security.log";

    // Intervalo de verificação do monitor (ms)
    public static final int MONITOR_INTERVAL_MS = 1000;

    // Tempo limite para considerar que uma thread está em Starvation (ms)
    public static final long STARVATION_THRESHOLD_MS = 3000;
}