package com.assistente.empresarial.service.structured;

public class LeadQualificadoDTO {

    private String nome;
    private String email;
    private String telefone;
    private String cargo;
    private String empresa;
    private String tamanhoEmpresa;
    private String nivelInteresse; // ALTO, MEDIO, BAIXO
    private String resumoNecessidade;
    private Double orcamentoEstimado;

    public LeadQualificadoDTO() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getTamanhoEmpresa() {
        return tamanhoEmpresa;
    }

    public void setTamanhoEmpresa(String tamanhoEmpresa) {
        this.tamanhoEmpresa = tamanhoEmpresa;
    }

    public String getNivelInteresse() {
        return nivelInteresse;
    }

    public void setNivelInteresse(String nivelInteresse) {
        this.nivelInteresse = nivelInteresse;
    }

    public String getResumoNecessidade() {
        return resumoNecessidade;
    }

    public void setResumoNecessidade(String resumoNecessidade) {
        this.resumoNecessidade = resumoNecessidade;
    }

    public Double getOrcamentoEstimado() {
        return orcamentoEstimado;
    }

    public void setOrcamentoEstimado(Double orcamentoEstimado) {
        this.orcamentoEstimado = orcamentoEstimado;
    }
}
