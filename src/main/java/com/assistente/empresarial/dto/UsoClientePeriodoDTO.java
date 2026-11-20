package com.assistente.empresarial.dto;

import com.assistente.empresarial.enuns.PlanoEmpresa;

import java.util.UUID;

public class UsoClientePeriodoDTO {
    private UUID empresaId;
    private String empresaNome;
    private PlanoEmpresa plano;
    private boolean status;
    private long tokensConsumidosPeriodo;
    private long requisicoesPeriodo;
    private double custoEstimadoUsd;
    private double latenciaMediaMs;
    private long conversasPeriodo;
    private long creditosInseridosPeriodo;
    private long creditosRestantes;
    private long tokensConsumidosTotal;

    public UUID getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(UUID empresaId) {
        this.empresaId = empresaId;
    }

    public String getEmpresaNome() {
        return empresaNome;
    }

    public void setEmpresaNome(String empresaNome) {
        this.empresaNome = empresaNome;
    }

    public PlanoEmpresa getPlano() {
        return plano;
    }

    public void setPlano(PlanoEmpresa plano) {
        this.plano = plano;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public long getTokensConsumidosPeriodo() {
        return tokensConsumidosPeriodo;
    }

    public void setTokensConsumidosPeriodo(long tokensConsumidosPeriodo) {
        this.tokensConsumidosPeriodo = tokensConsumidosPeriodo;
    }

    public long getRequisicoesPeriodo() {
        return requisicoesPeriodo;
    }

    public void setRequisicoesPeriodo(long requisicoesPeriodo) {
        this.requisicoesPeriodo = requisicoesPeriodo;
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

    public long getConversasPeriodo() {
        return conversasPeriodo;
    }

    public void setConversasPeriodo(long conversasPeriodo) {
        this.conversasPeriodo = conversasPeriodo;
    }

    public long getCreditosInseridosPeriodo() {
        return creditosInseridosPeriodo;
    }

    public void setCreditosInseridosPeriodo(long creditosInseridosPeriodo) {
        this.creditosInseridosPeriodo = creditosInseridosPeriodo;
    }

    public long getCreditosRestantes() {
        return creditosRestantes;
    }

    public void setCreditosRestantes(long creditosRestantes) {
        this.creditosRestantes = creditosRestantes;
    }

    public long getTokensConsumidosTotal() {
        return tokensConsumidosTotal;
    }

    public void setTokensConsumidosTotal(long tokensConsumidosTotal) {
        this.tokensConsumidosTotal = tokensConsumidosTotal;
    }
}
