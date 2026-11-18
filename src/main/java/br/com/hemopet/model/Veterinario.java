package br.com.hemopet.model;

public class Veterinario {
    private String cpf;
    private String crmv;
    private String cpfSupervisor;

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCrmv() {
        return crmv;
    }

    public void setCrmv(String crmv) {
        this.crmv = crmv;
    }

    public String getCpfSupervisor() {
        return cpfSupervisor;
    }

    public void setCpfSupervisor(String cpfSupervisor) {
        this.cpfSupervisor = cpfSupervisor;
    }
}
