package com.assistente.empresarial.model;

import com.assistente.empresarial.enuns.StatusConversa;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversas")
public class Conversa {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "assistente_id")
    private UUID assistenteId;

    @Column(name = "cliente_identificador")
    private String clienteIdentificador;

    @Column(name = "cliente_nome")
    private String clienteNome;

    @Column(name = "cliente_email")
    private String clienteEmail;

    @Column(name = "canal")
    private String canal = "WIDGET"; // WIDGET, WEB, WHATSAPP, INTERNO

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusConversa status = StatusConversa.BOT;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Conversa() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(UUID empresaId) {
        this.empresaId = empresaId;
    }

    public UUID getAssistenteId() {
        return assistenteId;
    }

    public void setAssistenteId(UUID assistenteId) {
        this.assistenteId = assistenteId;
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

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public StatusConversa getStatus() {
        return status;
    }

    public void setStatus(StatusConversa status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
