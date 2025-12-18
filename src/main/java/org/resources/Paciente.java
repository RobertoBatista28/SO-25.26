package org.resources;

public class Paciente implements Comparable<Paciente> {
    private String nome;
    private int prioridade; // 1=Urgente, 2=Normal, 3=Baixa

    public Paciente(String nome, int prioridade) {
        this.nome = nome;
        this.prioridade = prioridade;
    }

    public String getNome() { return nome; }
    public int getPrioridade() { return prioridade; }

    @Override
    public int compareTo(Paciente o) {
        return Integer.compare(this.prioridade, o.prioridade);
    }

    @Override
    public String toString() { return nome + " (Prio: " + prioridade + ")"; }
}