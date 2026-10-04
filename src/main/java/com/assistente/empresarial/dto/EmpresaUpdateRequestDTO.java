package com.assistente.empresarial.dto;

import jakarta.validation.constraints.NotBlank;

public class EmpresaUpdateRequestDTO {

    @NotBlank(message = "O nome da empresa é obrigatório.")
    private String nomeEmpresa;

    private String razaoSocial;

    public EmpresaUpdateRequestDTO() {
    }

    public EmpresaUpdateRequestDTO(String nomeEmpresa, String razaoSocial) {
        this.nomeEmpresa = nomeEmpresa;
        this.razaoSocial = razaoSocial;
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
}
