package br.com.hemopet.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AnimalDoador {
    private Integer idAnimal;
    private String nome;
    private String especie;
    private String raca;
    private String tipoSanguineo;
    private BigDecimal peso;
    private LocalDate dataNascimento;
    private boolean autorizacaoDoacao;
    private String cpfTutor;

    public Integer getIdAnimal() {
        return idAnimal;
    }

    public void setIdAnimal(Integer idAnimal) {
        this.idAnimal = idAnimal;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEspecie() {
        return especie;
    }

    public void setEspecie(String especie) {
        this.especie = especie;
    }

    public String getRaca() {
        return raca;
    }

    public void setRaca(String raca) {
        this.raca = raca;
    }

    public String getTipoSanguineo() {
        return tipoSanguineo;
    }

    public void setTipoSanguineo(String tipoSanguineo) {
        this.tipoSanguineo = tipoSanguineo;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public boolean isAutorizacaoDoacao() {
        return autorizacaoDoacao;
    }

    public void setAutorizacaoDoacao(boolean autorizacaoDoacao) {
        this.autorizacaoDoacao = autorizacaoDoacao;
    }

    public String getCpfTutor() {
        return cpfTutor;
    }

    public void setCpfTutor(String cpfTutor) {
        this.cpfTutor = cpfTutor;
    }
}
