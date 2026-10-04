package com.assistente.empresarial.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class ChatRequestDTO {

    private UUID conversaId;

    @NotBlank(message = "A mensagem não pode ser vazia.")
    private String mensagem;

    private String clienteIdentificador;
    private String clienteNome;
    private String clienteEmail;
    private UUID assistenteId;

    public ChatRequestDTO() {
    }

    public UUID getConversaId() {
        return conversaId;
    }

    public void setConversaId(UUID conversaId) {
        this.conversaId = conversaId;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getClienteIdentificador() {
        return clienteIdentificador;
    }

    public void setClienteIdentificador(String clienteIdentificador) {
        this.clienteIdentificador = clienteIdentificador;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }

    public UUID getAssistenteId() {
        return assistenteId;
    }

    public void setAssistenteId(UUID assistenteId) {
        this.assistenteId = assistenteId;
    }
}
