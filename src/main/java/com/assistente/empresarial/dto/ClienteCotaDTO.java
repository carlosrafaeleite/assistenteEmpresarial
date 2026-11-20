package com.assistente.empresarial.dto;

import com.assistente.empresarial.enuns.PlanoEmpresa;

import java.time.LocalDateTime;
import java.util.UUID;

public class ClienteCotaDTO {
    private UUID id;
    private String nome;
    private String razaoSocial;
    private String email;
    private String slug;
    private boolean status;
    private PlanoEmpresa plano;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Cotas de Créditos / Tokens
    private long creditosTokens;
    private long tokensConsumidos;
    private long creditosInseridosAdmin;
    private double percentualTokensUsados;
    private boolean alertaCreditoBaixo;

    // Cotas de Documentos e Armazenamento RAG
    private long documentosUsados;
    private int maxArquivos;
    private double percentualArquivosUsados;
    private long armazenamentoUsadoBytes;
    private long limiteArmazenamentoBytes;
    private double percentualArmazenamentoUsado;

    // Cotas de Assistentes
    private long assistentesUsados;
    private int limiteAssistentes;
    private double percentualAssistentesUsados;

    // Atividade Geral
    private long totalUsuarios;
    private long totalConversas;
    private long requisicoesPeriodo;
    private long tokensPeriodo;
    private double custoEstimadoPeriodoUsd;
    private LocalDateTime ultimaAtividade;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public PlanoEmpresa getPlano() {
        return plano;
    }

    public void setPlano(PlanoEmpresa plano) {
        this.plano = plano;
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

    public long getCreditosTokens() {
        return creditosTokens;
    }

    public void setCreditosTokens(long creditosTokens) {
        this.creditosTokens = creditosTokens;
    }

    public long getTokensConsumidos() {
        return tokensConsumidos;
    }

    public void setTokensConsumidos(long tokensConsumidos) {
        this.tokensConsumidos = tokensConsumidos;
    }

    public long getCreditosInseridosAdmin() {
        return creditosInseridosAdmin;
    }

    public void setCreditosInseridosAdmin(long creditosInseridosAdmin) {
        this.creditosInseridosAdmin = creditosInseridosAdmin;
    }

    public double getPercentualTokensUsados() {
        return percentualTokensUsados;
    }

    public void setPercentualTokensUsados(double percentualTokensUsados) {
        this.percentualTokensUsados = percentualTokensUsados;
    }

    public boolean isAlertaCreditoBaixo() {
        return alertaCreditoBaixo;
    }

    public void setAlertaCreditoBaixo(boolean alertaCreditoBaixo) {
        this.alertaCreditoBaixo = alertaCreditoBaixo;
    }

    public long getDocumentosUsados() {
        return documentosUsados;
    }

    public void setDocumentosUsados(long documentosUsados) {
        this.documentosUsados = documentosUsados;
    }

    public int getMaxArquivos() {
        return maxArquivos;
    }

    public void setMaxArquivos(int maxArquivos) {
        this.maxArquivos = maxArquivos;
    }

    public double getPercentualArquivosUsados() {
        return percentualArquivosUsados;
    }

    public void setPercentualArquivosUsados(double percentualArquivosUsados) {
        this.percentualArquivosUsados = percentualArquivosUsados;
    }

    public long getArmazenamentoUsadoBytes() {
        return armazenamentoUsadoBytes;
    }

    public void setArmazenamentoUsadoBytes(long armazenamentoUsadoBytes) {
        this.armazenamentoUsadoBytes = armazenamentoUsadoBytes;
    }

    public long getLimiteArmazenamentoBytes() {
        return limiteArmazenamentoBytes;
    }

    public void setLimiteArmazenamentoBytes(long limiteArmazenamentoBytes) {
        this.limiteArmazenamentoBytes = limiteArmazenamentoBytes;
    }

    public double getPercentualArmazenamentoUsado() {
        return percentualArmazenamentoUsado;
    }

    public void setPercentualArmazenamentoUsado(double percentualArmazenamentoUsado) {
        this.percentualArmazenamentoUsado = percentualArmazenamentoUsado;
    }

    public long getAssistentesUsados() {
        return assistentesUsados;
    }

    public void setAssistentesUsados(long assistentesUsados) {
        this.assistentesUsados = assistentesUsados;
    }

    public int getLimiteAssistentes() {
        return limiteAssistentes;
    }

    public void setLimiteAssistentes(int limiteAssistentes) {
        this.limiteAssistentes = limiteAssistentes;
    }

    public double getPercentualAssistentesUsados() {
        return percentualAssistentesUsados;
    }

    public void setPercentualAssistentesUsados(double percentualAssistentesUsados) {
        this.percentualAssistentesUsados = percentualAssistentesUsados;
    }

    public long getTotalUsuarios() {
        return totalUsuarios;
    }

    public void setTotalUsuarios(long totalUsuarios) {
        this.totalUsuarios = totalUsuarios;
    }

    public long getTotalConversas() {
        return totalConversas;
    }

    public void setTotalConversas(long totalConversas) {
        this.totalConversas = totalConversas;
    }

    public long getRequisicoesPeriodo() {
        return requisicoesPeriodo;
    }

    public void setRequisicoesPeriodo(long requisicoesPeriodo) {
        this.requisicoesPeriodo = requisicoesPeriodo;
    }

    public long getTokensPeriodo() {
        return tokensPeriodo;
    }

    public void setTokensPeriodo(long tokensPeriodo) {
        this.tokensPeriodo = tokensPeriodo;
    }

    public double getCustoEstimadoPeriodoUsd() {
        return custoEstimadoPeriodoUsd;
    }

    public void setCustoEstimadoPeriodoUsd(double custoEstimadoPeriodoUsd) {
        this.custoEstimadoPeriodoUsd = custoEstimadoPeriodoUsd;
    }

    public LocalDateTime getUltimaAtividade() {
        return ultimaAtividade;
    }

    public void setUltimaAtividade(LocalDateTime ultimaAtividade) {
        this.ultimaAtividade = ultimaAtividade;
    }
}
