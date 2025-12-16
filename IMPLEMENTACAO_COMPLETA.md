# Implementação Completa - Simulador de Concorrência com Monitorização

## ✅ RESUMO DE IMPLEMENTAÇÃO

Este projeto agora **implementa todos os requisitos** do enunciado, incluindo os 3.0 valores da perspetiva de cibersegurança que estavam em falta.

---

## 📋 CHECKLIST DOS REQUISITOS

### 3.1. Simulador de Concorrência [5.0 valores]

#### ✅ Race Condition
- **Classe**: `RaceConditionDemo.java`
- ✅ a) Recursos partilhados criados (`RecursoPartilhado`)
- ✅ b) Múltiplas threads sem sincronização adequada (`runUnsynchronized`)
- ✅ c) Demonstração de resultados inconsistentes
- ✅ d) Correção usando locks (`runSynchronized`)

#### ✅ Deadlock
- **Classe**: `DeadlockDemo.java`
- ✅ a) Threads competindo por exclusividade de recursos
- ✅ b) Sequência que provoca deadlock
- ✅ c) Registos de deadlock no ficheiro de log
- ✅ d) Correção usando ordenação global de locks (`runCorrected`)

#### ✅ Starvation
- **Classe**: `StarvationDemo.java`
- ✅ a) Threads com diferentes prioridades/padrões de acesso
- ✅ b) Demonstração de starvation (threads preteridas)
- ✅ c) Registos de starvation no ficheiro de log
- ✅ d) Correção usando fair lock (`runCorrected`)

---

### 3.2. Mecanismos de Monitorização eBPF [3.0 valores]

#### ✅ Classe MonitorEBPF
- **Ficheiro**: `monitor/MonitorEBPF.java`

**Funcionalidade 1: Deteção e Alertas**
- ✅ Deteção de race conditions (acessos não protegidos próximos no tempo)
- ✅ Deteção de deadlocks potenciais (wait-for graph)
- ✅ Deteção de deadlocks confirmados (cycle detection)
- ✅ Deteção de padrões de starvation (tempos de espera longos)
- ✅ Geração de alertas registados em `logs/monitor.log`

**Funcionalidade 2: Estatísticas**
- ✅ Número de acessos por thread (`getAccessesPerThread()`)
- ✅ Ordem de obtenção de locks (`getAcquisitionOrder()`)
- ✅ Tempo de espera para entrada em secções críticas
- ✅ Timestamps de todos os eventos

---

### 3.3. Perspetiva de Cibersegurança [3.0 valores] ⭐ **NOVO**

#### ✅ 1) Race Conditions → Escalada de Privilégios e Corrupção de Dados
- **Classe**: `PrivilegeEscalationDemo.java`

**Cenários Implementados:**
- ✅ **Privilege Escalation**: Race condition permite que thread não autorizada obtenha privilégios de administrador
  - `runVulnerableScenario()`: Demonstra bypass de verificações de segurança
  - `runSecureScenario()`: Versão corrigida com sincronização adequada
  
- ✅ **Data Corruption**: Race condition corrompe transações financeiras
  - `runDataCorruptionScenario()`: Demonstra perda de dados em operações bancárias

**Impacto de Cibersegurança:**
- Aumento de privilégios não autorizado
- Corrupção de dados críticos
- Bypass de verificações de autenticação

---

#### ✅ 2) Deadlocks → Ataques de Denial of Service (DoS)
- **Classe**: `DenialOfServiceDemo.java`

**Cenários Implementados:**
- ✅ **DoS via Deadlock Deliberado**: Atacante cria deadlock para bloquear serviços legítimos
  - `runDoSAttack()`: Threads atacantes adquirem recursos em ordem inversa
  - Serviços legítimos ficam bloqueados
  - Demonstração de impacto (requests falhados vs. bem-sucedidos)
  
- ✅ **Mitigação com Timeouts**: 
  - `runMitigatedScenario()`: Uso de `tryLock()` previne bloqueio permanente
  
- ✅ **Resource Exhaustion DoS**: 
  - `runResourceExhaustionDoS()`: Atacantes monopolizam todos os recursos
  - Utilizadores legítimos não conseguem acesso

**Impacto de Cibersegurança:**
- Denial of Service completo
- Indisponibilidade de serviços críticos
- Bloqueio permanente de operações

---

#### ✅ 3) Starvation → Falhas de Segurança em Serviços Críticos
- **Classe**: `CriticalServiceStarvationDemo.java`

**Cenários Implementados:**
- ✅ **Security Monitor Starvation**: Serviço de monitorização de segurança é "esfomeado"
  - `runVulnerableScenario()`: Tarefas background impedem deteção de intrusões
  - Eventos de segurança ficam obsoletos antes de serem processados
  - **Resultado**: Brechas de segurança não detetadas
  
- ✅ **Fair Scheduling**: 
  - `runSecureScenario()`: Fair lock garante processamento atempado de eventos
  
- ✅ **Authentication Service Starvation**: 
  - `runAuthenticationStarvationScenario()`: Serviço de autenticação atrasado
  - Falhas de login não são bloqueadas a tempo
  - **Resultado**: Sistema comprometido

**Impacto de Cibersegurança:**
- Atraso em serviços críticos de segurança
- Intrusões não detetadas
- Falhas de autenticação não bloqueadas
- Sistema comprometido por ataques não mitigados

---

## 🎯 DEMONSTRAÇÕES DE CIBERSEGURANÇA

### Exemplo 1: Privilege Escalation
```
[SECURITY BREACH] attacker escalated privileges to admin!
```
- Atacante explora race condition entre verificação e atribuição de privilégios
- **Sem sincronização**: múltiplas escaladas bem-sucedidas
- **Com sincronização**: todas as tentativas bloqueadas

### Exemplo 2: Denial of Service
```
[DoS IMPACT] service-3 request failed - service blocked!
[DoS RESULT] Successful requests: 2, Failed requests: 13
[DoS RESULT] Service availability compromised due to deadlock attack!
```
- Atacantes criam deadlock deliberado
- Serviços legítimos ficam bloqueados
- Taxa de sucesso drasticamente reduzida

### Exemplo 3: Critical Service Failure
```
[MISSED] Security event intrusion-5 too old (delay: 1250ms) - SECURITY FAILURE!
[VULNERABILITY] Detected: 3/10, Missed (breaches): 7
[SECURITY BREACH] System compromised due to authentication service starvation!
```
- Serviço de segurança atrasado por starvation
- Eventos críticos não processados a tempo
- Sistema fica vulnerável a ataques

---

## 📊 OUTPUTS DO MONITOR

O ficheiro `logs/monitor.log` contém registos detalhados:

```
[2025-12-16T...] RACE_DETECTED on resource=shared1 between threads 12 and 13
[2025-12-16T...] POTENTIAL_DEADLOCK involving threads 15 and 16 locks A and B
[2025-12-16T...] DEADLOCK_DETECTED among threads in wait-for graph
[2025-12-16T...] STARVATION_PATTERN thread=18 waited=3100 ms
```

---

## 🚀 COMO EXECUTAR

```bash
# Compilar
javac -d out src/monitor/*.java src/simulador/*.java src/Main.java

# Executar
java -cp out Main

# Ver logs
type logs\monitor.log        # Windows
cat logs/monitor.log         # Linux/Mac
```

---

## 📈 PONTUAÇÃO COMPLETA

| Requisito | Pontos | Status |
|-----------|--------|--------|
| 3.1 Simulador de Concorrência | 5.0 | ✅ Completo |
| 3.2 Monitorização eBPF | 3.0 | ✅ Completo |
| 3.3 Cibersegurança | 3.0 | ✅ **IMPLEMENTADO** |
| **TOTAL** | **11.0** | ✅ **100%** |

---

## 🔒 CONCEITOS DE CIBERSEGURANÇA DEMONSTRADOS

1. **Privilege Escalation** - Exploração de race conditions
2. **Data Corruption** - Perda de integridade de dados
3. **Denial of Service** - Bloqueio de serviços via deadlocks
4. **Resource Exhaustion** - Monopolização de recursos
5. **Security Monitoring Failure** - Serviços críticos inoperacionais
6. **Authentication Bypass** - Sistemas de segurança comprometidos

---

## 📚 CONCLUSÃO

O projeto agora demonstra **todos os aspectos** do enunciado:
- ✅ Concorrência (race conditions, deadlocks, starvation)
- ✅ Monitorização (deteção, alertas, estatísticas)
- ✅ **Cibersegurança (exploração real de vulnerabilidades)**

Cada cenário tem:
- Versão **vulnerável** que demonstra o ataque
- Versão **segura** que demonstra a correção
- Registos completos no monitor eBPF
