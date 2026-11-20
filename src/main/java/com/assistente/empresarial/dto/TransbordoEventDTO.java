package com.assistente.empresarial.dto;

import com.assistente.empresarial.enuns.StatusConversa;

import java.time.LocalDateTime;
import java.util.UUID;

public class TransbordoEventDTO {

    private String tipoEvento;
    private UUID conversaId;
    private UUID empresaId;
    private String clienteNome;
    private StatusConversa status;
    private String mensagem;
    private String remetente;
    private LocalDateTime timestamp;

    public TransbordoEventDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public TransbordoEventDTO(String tipoEvento, UUID conversaId, UUID empresaId, String clienteNome,
                              StatusConversa status, String mensagem, String remetente) {
        this.tipoEvento = tipoEvento;
        this.conversaId = conversaId;
        this.empresaId = empresaId;
        this.clienteNome = clienteNome;
        this.status = status;
        this.mensagem = mensagem;
        this.remetente = remetente;
        this.timestamp = LocalDateTime.now();
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public UUID getConversaId() {
        return conversaId;
    }

    public void setConversaId(UUID conversaId) {
        this.conversaId = conversaId;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(UUID empresaId) {
        this.empresaId = empresaId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public StatusConversa getStatus() {
        return status;
    }

    public void setStatus(StatusConversa status) {
        this.status = status;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getRemetente() {
        return remetente;
    }

    public void setRemetente(String remetente) {
        this.remetente = remetente;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
