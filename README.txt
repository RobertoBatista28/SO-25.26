
Projeto: Simulador de concorrência com monitorização (Sistemas Operativos)

=======================================================================
ESTRUTURA DO PROJETO
=======================================================================

src/
├── Main.java                                    : Ponto de entrada principal
├── monitor/
│   └── MonitorEBPF.java                        : Monitorização estilo eBPF
└── simulador/
    ├── RecursoPartilhado.java                  : Recurso partilhado básico
    ├── RaceConditionDemo.java                  : Demo de race conditions
    ├── DeadlockDemo.java                       : Demo de deadlocks
    ├── StarvationDemo.java                     : Demo de starvation
    ├── PrivilegeEscalationDemo.java            : Escalada de privilégios
    ├── DenialOfServiceDemo.java                : Ataques DoS via deadlock
    └── CriticalServiceStarvationDemo.java      : Falhas de segurança

=======================================================================
IMPLEMENTAÇÃO DOS REQUISITOS DO ENUNCIADO
=======================================================================

✅ 3.1. SIMULADOR DE CONCORRÊNCIA [5.0 valores]
   - Race conditions (versão não sincronizada e corrigida)
   - Deadlocks (cenário problemático e versão corrigida)
   - Starvation (cenário problemático e versão corrigida)
   - Registos em logs de todas as ocorrências

✅ 3.2. MECANISMOS DE MONITORIZAÇÃO EBPF [3.0 valores]
   - Deteção de race conditions, deadlocks e starvation
   - Alertas registados em ficheiro de log
   - Estatísticas: acessos por thread, ordem de aquisição, tempos de espera

✅ 3.3. PERSPETIVA DE CIBERSEGURANÇA [3.0 valores]
   ① Race conditions → Escalada de privilégios + Corrupção de dados
   ② Deadlocks → Ataques de Denial of Service (DoS)
   ③ Starvation → Atrasos em serviços críticos de segurança

=======================================================================
COMO COMPILAR E EXECUTAR
=======================================================================

Requisitos: JDK 17+

Opção 1 (recomendada):
  javac -d out src/monitor/*.java src/simulador/*.java src/Main.java
  java -cp out Main

Opção 2:
  javac -d out src/**/*.java
  java -cp out Main

=======================================================================
SAÍDA
=======================================================================

Os logs são criados em: ./logs/monitor.log

O programa executa todos os cenários automaticamente e apresenta:
- Demonstrações de race conditions, deadlocks e starvation
- Cenários de cibersegurança (privilege escalation, DoS, critical failures)
- Estatísticas completas do monitor eBPF
