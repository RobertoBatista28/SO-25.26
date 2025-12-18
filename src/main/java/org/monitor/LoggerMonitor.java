package org.monitor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerMonitor {
    private final String filepath;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public LoggerMonitor(String filepath) {
        this.filepath = filepath;
        File f = new File(filepath);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
    }

    public synchronized void log(String message) {
        String entry = String.format("[%s] %s", dtf.format(LocalDateTime.now()), message);
        System.out.println(entry);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath, true))) {
            writer.write(entry);
            writer.newLine();
        } catch (IOException e) { System.err.println("Erro Log: " + e.getMessage()); }
    }
}