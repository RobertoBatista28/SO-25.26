Como seu docente, analisei o seu `README.md` e identifiquei algumas imprecisões técnicas em relação à implementação real do seu código e aos conceitos teóricos lecionados.

A principal correção necessária refere-se ao **MonitorEBPF**: o seu código **não utiliza** `ThreadMXBean`. Em vez disso, implementou uma monitorização por **instrumentação (hooks/probes)** onde os recursos notificam o monitor, e a deteção de deadlocks é feita manualmente através de um algoritmo de procura de ciclos (DFS) num **Wait-for Graph**.

Aqui está a versão corrigida e tecnicamente rigorosa, mantendo o seu estilo:

---

# Simulador de Concorrência com Monitorização (eBPF Style)

Este projeto é um simulador em Java desenvolvido para a unidade curricular de **Sistemas Operativos**. O objetivo é demonstrar e mitigar vulnerabilidades críticas de concorrência — **Race Conditions**, **Deadlocks**, **Starvation** e **Ordem de Execução** — sob a vigilância de um monitor que simula o comportamento da tecnologia **eBPF** no kernel.

---

## Como Executar o Projeto

Certifique-se de que tem o **Java JDK 17** ou superior instalado.

### Opção 1: Via VS Code (Recomendado)

1. Abra a pasta raiz do projeto no **VS Code**.
2. Navegue no explorador até `src/main/java/org/app/Main.java`.
3. Clique em **"Run"** acima do método `main` ou pressione `F5`.

### Opção 2: Via Terminal (Linha de Comandos)

A partir da raiz do projeto:

1. **Compilar:**
```bash
javac -d bin src/main/java/org/app/*.java src/main/java/org/monitor/*.java src/main/java/org/resources/*.java src/main/java/org/scenarios/*.java src/main/java/org/solutions/*.java

```


2. **Executar:**
```bash
java -cp bin org.app.Main

```



---

## Estrutura do Pacotes

O código segue a organização académica exigida:

* **`org.app`**: Ponto de entrada (`Main`), configurações globais e menus do sistema.
* **`org.monitor`**: Núcleo de monitorização (`MonitorEBPF`), gestão de logs e o `DetectorDeadlock` (implementação de grafos).
* **`org.resources`**: Recursos partilhados instrumentados para comunicar com o monitor (ex: `BaseDados`, `StockSangue`).
* 
**`org.scenarios`**: Cenários de falha para testes de cibersegurança (Red Team).


* 
**`org.solutions`**: Implementação de mecanismos de sincronização seguros (Blue Team).



---

## Cenários e Mecanismos de Sincronização

### Menu Inseguro (Vulnerabilidades)

Demonstração de comportamentos anómalos em sistemas multiprogramados:

1. 
**Race Condition:** Falta de exclusão mútua resultando em inconsistência de dados (ex: stock negativo).


2. 
**Deadlock:** Simulação de um ataque de Negação de Serviço (DoS) através de espera circular por recursos.


3. 
**Starvation:** Threads de baixa prioridade preteridas indefinidamente por um fluxo contínuo de alta prioridade.


4. 
**Ordem Conflituante:** Falha na coordenação de tarefas interdependentes (ex: cirurgia sem anestesia).



### Menu Seguro (Soluções Académicas)

1. 
**Exclusão Mútua:** Uso de métodos `synchronized` ou **Semáforos** (`acquire`/`release`) para proteger secções críticas.


2. 
**Ordenação de Recursos:** Prevenção de Deadlocks através da hierarquia de aquisição.


3. **Justiça (Fairness):** Mitigação de Starvation através de `ReentrantLock(true)` (Fair Locks) para garantir o progresso.
4. **Sincronização Temporal:** Uso de `thread.join()` para garantir a ordem correta de execução.

---

## Monitorização ao Estilo eBPF

O `MonitorEBPF` atua como um mecanismo de monitorização não invasivo:

* **Instrumentação (Hooks):** Os recursos notificam o monitor em eventos de *Request*, *Use* e *Release*.
* **Wait-for Graph:** O `DetectorDeadlock` constrói dinamicamente um grafo de dependências e utiliza **DFS (Procura em Profundidade)** para identificar ciclos de espera circular.
* **Análise de Starvation:** Monitoriza threads nos estados `BLOCKED` ou `WAITING` e gera alertas caso excedam o tempo limite (`threshold`).
* 
**Logs de Auditoria:** Regista estatísticas de acesso, ordem de eventos e tempos de espera num ficheiro de log específico para análise de cibersegurança.



---

## Autores

* **Paulo Neto** - 8230679
* **Roberto Baptista** - 8230471
* **João Coelho** - 8230465