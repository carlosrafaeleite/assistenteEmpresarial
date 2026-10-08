package com.assistente.empresarial.model;

import com.assistente.empresarial.enuns.StatusDocumento;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documentos")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "assistente_id")
    private UUID assistenteId;

    @Column(name = "nome_original", nullable = false)
    private String nomeOriginal;

    @Column(name = "nome_salvo", nullable = false)
    private String nomeSalvo;

    @Column(name = "caminho_arquivo", nullable = false)
    private String caminhoArquivo;

    @Column(name = "tipo_conteudo")
    private String tipoConteudo;

    @Column(name = "tamanho_bytes")
    private long tamanhoBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDocumento status = StatusDocumento.PENDENTE;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;

    @Column(name = "total_chunks")
    private int totalChunks = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Documento() {
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

    public String getNomeOriginal() {
        return nomeOriginal;
    }

    public void setNomeOriginal(String nomeOriginal) {
        this.nomeOriginal = nomeOriginal;
    }

    public String getNomeSalvo() {
        return nomeSalvo;
    }

    public void setNomeSalvo(String nomeSalvo) {
        this.nomeSalvo = nomeSalvo;
    }

    public String getCaminhoArquivo() {
        return caminhoArquivo;
    }

    public void setCaminhoArquivo(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
    }

    public String getTipoConteudo() {
        return tipoConteudo;
    }

    public void setTipoConteudo(String tipoConteudo) {
        this.tipoConteudo = tipoConteudo;
    }

    public long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public StatusDocumento getStatus() {
        return status;
    }

    public void setStatus(StatusDocumento status) {
        this.status = status;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }

    public void setMensagemErro(String mensagemErro) {
        this.mensagemErro = mensagemErro;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
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
