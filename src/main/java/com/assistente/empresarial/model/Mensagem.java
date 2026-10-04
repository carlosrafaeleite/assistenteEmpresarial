package com.assistente.empresarial.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mensagens")
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "conversa_id", nullable = false)
    private UUID conversaId;

    @Column(nullable = false)
    private String remetente; // USUARIO, ASSISTENTE, HUMANO

    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Column(name = "fontes", columnDefinition = "TEXT")
    private String fontes; // Nomes dos documentos usados como fonte no RAG

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Mensagem() {
    }

    public Mensagem(UUID conversaId, String remetente, String conteudo) {
        this.conversaId = conversaId;
        this.remetente = remetente;
        this.conteudo = conteudo;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getConversaId() {
        return conversaId;
    }

    public void setConversaId(UUID conversaId) {
        this.conversaId = conversaId;
    }

    public String getRemetente() {
        return remetente;
    }

    public void setRemetente(String remetente) {
        this.remetente = remetente;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public String getFontes() {
        return fontes;
    }

    public void setFontes(String fontes) {
        this.fontes = fontes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
