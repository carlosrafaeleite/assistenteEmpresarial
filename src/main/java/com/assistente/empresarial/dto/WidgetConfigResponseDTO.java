package com.assistente.empresarial.dto;

import java.util.UUID;

public class WidgetConfigResponseDTO {

    private UUID assistenteId;
    private String assistenteNome;
    private String empresaNome;
    private String empresaSlug;
    private String corPrimaria;
    private String corSecundaria;
    private String avatarUrl;
    private String mensagemBoasVindas;
    private String tomVoz;
    private String nomeBot;

    public WidgetConfigResponseDTO() {
    }

    public UUID getAssistenteId() {
        return assistenteId;
    }

    public void setAssistenteId(UUID assistenteId) {
        this.assistenteId = assistenteId;
    }

    public String getAssistenteNome() {
        return assistenteNome;
    }

    public void setAssistenteNome(String assistenteNome) {
        this.assistenteNome = assistenteNome;
    }

    public String getEmpresaNome() {
        return empresaNome;
    }

    public void setEmpresaNome(String empresaNome) {
        this.empresaNome = empresaNome;
    }

    public String getEmpresaSlug() {
        return empresaSlug;
    }

    public void setEmpresaSlug(String empresaSlug) {
        this.empresaSlug = empresaSlug;
    }

    public String getCorPrimaria() {
        return corPrimaria;
    }

    public void setCorPrimaria(String corPrimaria) {
        this.corPrimaria = corPrimaria;
    }

    public String getCorSecundaria() {
        return corSecundaria;
    }

    public void setCorSecundaria(String corSecundaria) {
        this.corSecundaria = corSecundaria;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getMensagemBoasVindas() {
        return mensagemBoasVindas;
    }

    public void setMensagemBoasVindas(String mensagemBoasVindas) {
        this.mensagemBoasVindas = mensagemBoasVindas;
    }

    public String getTomVoz() {
        return tomVoz;
    }

    public void setTomVoz(String tomVoz) {
        this.tomVoz = tomVoz;
    }

    public String getNomeBot() {
        return nomeBot;
    }

    public void setNomeBot(String nomeBot) {
        this.nomeBot = nomeBot;
    }
}
