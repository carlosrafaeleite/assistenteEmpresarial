package com.assistente.empresarial.dto;

public class ValidarSenhaDTO {

    private String senha;

    public ValidarSenhaDTO() {
    }

    public ValidarSenhaDTO(String senha) {
        this.senha = senha;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
