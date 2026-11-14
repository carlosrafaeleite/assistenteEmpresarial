package com.assistente.empresarial.dto;

public class AssistenteSenhaDTO {

    private String senha;

    public AssistenteSenhaDTO() {
    }

    public AssistenteSenhaDTO(String senha) {
        this.senha = senha;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
