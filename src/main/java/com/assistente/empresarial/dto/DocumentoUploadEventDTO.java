package com.assistente.empresarial.dto;

import java.io.Serializable;
import java.util.UUID;

public class DocumentoUploadEventDTO implements Serializable {

    private UUID documentoId;
    private UUID empresaId;
    private UUID assistenteId;
    private String caminhoArquivo;

    public DocumentoUploadEventDTO() {
    }

    public DocumentoUploadEventDTO(UUID documentoId, UUID empresaId, UUID assistenteId, String caminhoArquivo) {
        this.documentoId = documentoId;
        this.empresaId = empresaId;
        this.assistenteId = assistenteId;
        this.caminhoArquivo = caminhoArquivo;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(UUID documentoId) {
        this.documentoId = documentoId;
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

    public String getCaminhoArquivo() {
        return caminhoArquivo;
    }

    public void setCaminhoArquivo(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
    }
}
