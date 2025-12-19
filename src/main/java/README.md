# Simulador de Concorrência com Monitorização (eBPF Style)

Este projeto é um simulador em Java desenvolvido no âmbito da unidade curricular de **Sistemas Operativos**. O objetivo é demonstrar vulnerabilidades clássicas de concorrência (Race Conditions, Deadlocks, Starvation e Execution Order) e como mitigá-las utilizando técnicas de solução desses problemas, sob a vigilância de um monitor inspirado na tecnologia **eBPF**.

---

## Como Executar o Projeto

Certificar se tem o **Java JDK** instalado (recomendado JDK 17 ou superior).

### Opção 1: Via VS Code (Recomendado)
1. Abra a pasta do projeto no **VS Code**.
2. Certifique-se de que a extensão "Extension Pack for Java" está instalada.
3. No explorador de ficheiros, navegue até `src/org/app/Main.java`.
4. Abra o ficheiro e clique em **"Run"** (ou "Executar") que aparece acima do método `main`, ou pressione `F5`.

### Opção 2: Via Terminal (Linha de Comandos)
Para compilar e executar manualmente, siga estes passos a partir da raiz do projeto:

1. **Navegar para a pasta do código fonte:**
    ```bash
    cd src

2. **Compilar o código: (Este comando compila o Main e todas as dependências automaticamente)**
    javac org/app/Main.java

3. **Executar a aplicação:**
    java org.app.Main

--

## Estrutura do Projeto

O código está organizado nos seguintes pacotes:

*   **`org.app`**: Contém a classe `Main` (CLI e Menus).
*   **`org.monitor`**: Contém o `MonitorEBPF` (Singleton) que audita as threads em background.
*   **`org.resources`**: Recursos partilhados simulados (ex: `StockSangue`, `BaseDados`).
*   **`org.scenarios`** (Red Team): Implementação das vulnerabilidades e ataques.
*   **`org.solutions`** (Blue Team): Implementação das correções e código seguro.

--

## Funcionalidades e Cenários

O simulador permite alternar entre dois modos de operação:

### Menu Inseguro (Vulnerabilidades)
Simula falhas graves de sistemas operativos e concorrência:

1. **Race Condition (Stock de Sangue):**
   * **Problema:** Acesso simultâneo de escrita sem exclusão mútua.
   * **Consequência:** Corrupção de dados (Stock fica negativo).

2. **Deadlock (Base de Dados):**
   * **Problema:** Espera circular na aquisição de bloqueios em tabelas cruzadas (A espera por B, B espera por A).
   * **Consequência:** O sistema bloqueia (*hang*) e entra em Negação de Serviço (DoS).

3. **Starvation (Atendimento/Triagem):**
   * **Problema:** Inundação (*flooding*) de threads de alta prioridade.
   * **Consequência:** Threads de baixa prioridade sofrem inanição e não são executadas.

4. **Ordem Conflituante (Cirurgia):**
   * **Problema:** Falta de coordenação temporal entre tarefas interdependentes.
   * **Consequência:** Execução ilógica (ex: Cirurgia começa antes da Anestesia).

### Menu Seguro (Soluções)
Aplica correções utilizando mecanismos de sincronização:

1. **Atomicidade:** Uso de blocos `synchronized` para garantir integridade do stock.
2. **Ordenação de Recursos:** Prevenção de Deadlock impondo uma ordem global de aquisição de *locks*.
3. **Justiça (Fairness):** Uso de `PriorityBlockingQueue` ou *Fair Locks* para garantir que todas as threads são atendidas.
4. **Coordenação:** Uso de `thread.join()` para garantir a sequencialidade correta dos processos.

---

## Monitorização (Estilo eBPF)

O projeto inclui uma classe `MonitorEBPF` que corre numa thread independente. Tal como o eBPF no kernel Linux, este monitor:

* Inspeciona o estado da JVM (via `ThreadMXBean`) periodicamente.
* Deteta ciclos de **Deadlock** analisando o grafo de espera.
* Mede a latência das threads para identificar **Starvation**.
* Emite alertas em tempo real na consola e, em casos críticos (como Deadlock), intervém para desbloquear o sistema.

---

## Autores

* **Paulo Neto** - 8230679
* **Roberto Baptista** - 8230471
* **João Coelho** - 8230465