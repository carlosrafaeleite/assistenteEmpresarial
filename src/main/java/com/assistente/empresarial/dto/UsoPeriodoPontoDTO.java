package com.assistente.empresarial.dto;

public class UsoPeriodoPontoDTO {
    private String data;
    private String label;
    private long tokensInput;
    private long tokensOutput;
    private long tokensTotal;
    private long requisicoes;
    private long cacheHits;
    private long fallbacks;
    private double custoEstimadoUsd;
    private double latenciaMediaMs;
    private long creditosInseridos;
    private long creditosRemovidos;
    private long conversasCriadas;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getTokensInput() {
        return tokensInput;
    }

    public void setTokensInput(long tokensInput) {
        this.tokensInput = tokensInput;
    }

    public long getTokensOutput() {
        return tokensOutput;
    }

    public void setTokensOutput(long tokensOutput) {
        this.tokensOutput = tokensOutput;
    }

    public long getTokensTotal() {
        return tokensTotal;
    }

    public void setTokensTotal(long tokensTotal) {
        this.tokensTotal = tokensTotal;
    }

    public long getRequisicoes() {
        return requisicoes;
    }

    public void setRequisicoes(long requisicoes) {
        this.requisicoes = requisicoes;
    }

    public long getCacheHits() {
        return cacheHits;
    }

    public void setCacheHits(long cacheHits) {
        this.cacheHits = cacheHits;
    }

    public long getFallbacks() {
        return fallbacks;
    }

    public void setFallbacks(long fallbacks) {
        this.fallbacks = fallbacks;
    }

    public double getCustoEstimadoUsd() {
        return custoEstimadoUsd;
    }

    public void setCustoEstimadoUsd(double custoEstimadoUsd) {
        this.custoEstimadoUsd = custoEstimadoUsd;
    }

    public double getLatenciaMediaMs() {
        return latenciaMediaMs;
    }

    public void setLatenciaMediaMs(double latenciaMediaMs) {
        this.latenciaMediaMs = latenciaMediaMs;
    }

    public long getCreditosInseridos() {
        return creditosInseridos;
    }

    public void setCreditosInseridos(long creditosInseridos) {
        this.creditosInseridos = creditosInseridos;
    }

    public long getCreditosRemovidos() {
        return creditosRemovidos;
    }

    public void setCreditosRemovidos(long creditosRemovidos) {
        this.creditosRemovidos = creditosRemovidos;
    }

    public long getConversasCriadas() {
        return conversasCriadas;
    }

    public void setConversasCriadas(long conversasCriadas) {
        this.conversasCriadas = conversasCriadas;
    }
}
