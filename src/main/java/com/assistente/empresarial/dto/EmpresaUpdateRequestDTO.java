package com.assistente.empresarial.dto;

import jakarta.validation.constraints.NotBlank;

public class EmpresaUpdateRequestDTO {

    @NotBlank(message = "O nome da empresa é obrigatório.")
    private String nomeEmpresa;

    private String razaoSocial;

    private String nomeBot;

    public EmpresaUpdateRequestDTO() {
    }

    public EmpresaUpdateRequestDTO(String nomeEmpresa, String razaoSocial, String nomeBot) {
        this.nomeEmpresa = nomeEmpresa;
        this.razaoSocial = razaoSocial;
        this.nomeBot = nomeBot;
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public void setNomeEmpresa(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getNomeBot() {
        return nomeBot;
    }

    public void setNomeBot(String nomeBot) {
        this.nomeBot = nomeBot;
    }
}
