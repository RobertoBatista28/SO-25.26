
import monitor.MonitorEBPF;
import simulador.*;

import java.io.IOException;

/**
 * Main runner that executes the required simulation scenarios.
 * Produces monitor.log in the working directory.
 * 
 * Includes:
 * - 3.1 Simulador de concorrência (race conditions, deadlocks, starvation)
 * - 3.2 Mecanismos de Monitorização ao estilo eBPF
 * - 3.3 Perspetiva de cibersegurança (privilege escalation, DoS, critical service failures)
 */
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        String logfile = "logs/monitor.log";
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get("logs"));
        MonitorEBPF monitor = new MonitorEBPF(logfile);

        System.out.println("==========================================================");
        System.out.println("    SIMULADOR DE CONCORRÊNCIA COM MONITORIZAÇÃO");
        System.out.println("==========================================================\n");

        // ===================================================================
        // 3.1 SIMULADOR DE CONCORRÊNCIA
        // ===================================================================
        System.out.println("\n### 3.1 SIMULADOR DE CONCORRÊNCIA ###\n");
        
        // 1) Race condition demo
        RecursoPartilhado shared1 = new RecursoPartilhado("shared1", monitor);
        RaceConditionDemo race = new RaceConditionDemo(shared1, monitor);
        race.runUnsynchronized(10, 100); // expect races: 1000 expected but less due to races

        // Reset resource for synchronized run
        RecursoPartilhado shared1_corrected = new RecursoPartilhado("shared1_corrected", monitor);
        RaceConditionDemo race_corrected = new RaceConditionDemo(shared1_corrected, monitor);
        race_corrected.runSynchronized(10, 100);

        // 2) Deadlock demo
        DeadlockDemo dead = new DeadlockDemo(monitor);
        dead.runDeadlockScenario();
        dead.runCorrected(4);

        // 3) Starvation demo
        StarvationDemo starv = new StarvationDemo(monitor);
        starv.runStarvationScenario();
        starv.runCorrected();

        // ===================================================================
        // 3.3 PERSPETIVA DE CIBERSEGURANÇA
        // ===================================================================
        System.out.println("\n### 3.3 PERSPETIVA DE CIBERSEGURANÇA ###\n");
        
        // 1) Race conditions que levam a aumento de privilégios e corrupção de dados
        System.out.println("\n--- 3.3.1 Race Conditions: Privilege Escalation & Data Corruption ---\n");
        PrivilegeEscalationDemo privEscalation = new PrivilegeEscalationDemo(monitor);
        privEscalation.runVulnerableScenario();
        Thread.sleep(500);
        privEscalation.runSecureScenario();
        Thread.sleep(500);
        privEscalation.runDataCorruptionScenario();
        
        // 2) Deadlocks explorados como ataques de DoS (Denial of Service)
        System.out.println("\n--- 3.3.2 Deadlocks: Denial of Service (DoS) Attacks ---\n");
        DenialOfServiceDemo dosDemo = new DenialOfServiceDemo(monitor);
        dosDemo.runDoSAttack();
        Thread.sleep(500);
        dosDemo.runMitigatedScenario();
        Thread.sleep(500);
        dosDemo.runResourceExhaustionDoS();
        
        // 3) Starvation que atrasa serviços críticos, causando falhas de segurança
        System.out.println("\n--- 3.3.3 Starvation: Critical Service Delays & Security Failures ---\n");
        CriticalServiceStarvationDemo criticalStarv = new CriticalServiceStarvationDemo(monitor);
        criticalStarv.runVulnerableScenario();
        Thread.sleep(500);
        criticalStarv.runSecureScenario();
        Thread.sleep(500);
        criticalStarv.runAuthenticationStarvationScenario();

        // ===================================================================
        // 3.2 ESTATÍSTICAS DO MONITOR EBPF
        // ===================================================================
        System.out.println("\n### 3.2 ESTATÍSTICAS DO MONITOR EBPF ###");
        System.out.println("=== Monitor statistics ===");
        System.out.println("Accesses per thread: " + monitor.getAccessesPerThread());
        System.out.println("Acquisition order: " + monitor.getAcquisitionOrder());

        monitor.shutdown();
        System.out.println("\n==========================================================");
        System.out.println("Simulação concluída! Logs escritos em: " + logfile);
        System.out.println("==========================================================");
    }
}
