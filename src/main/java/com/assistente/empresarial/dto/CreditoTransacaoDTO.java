package com.assistente.empresarial.dto;

import com.assistente.empresarial.enuns.TipoTransacaoCredito;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreditoTransacaoDTO {
    private UUID id;
    private UUID empresaId;
    private String empresaNome;
    private TipoTransacaoCredito tipo;
    private long quantidadeTokens;
    private long saldoAnterior;
    private long saldoPosterior;
    private String motivo;
    private String realizadoPorEmail;
    private LocalDateTime createdAt;

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

    public String getEmpresaNome() {
        return empresaNome;
    }

    public void setEmpresaNome(String empresaNome) {
        this.empresaNome = empresaNome;
    }

    public TipoTransacaoCredito getTipo() {
        return tipo;
    }

    public void setTipo(TipoTransacaoCredito tipo) {
        this.tipo = tipo;
    }

    public long getQuantidadeTokens() {
        return quantidadeTokens;
    }

    public void setQuantidadeTokens(long quantidadeTokens) {
        this.quantidadeTokens = quantidadeTokens;
    }

    public long getSaldoAnterior() {
        return saldoAnterior;
    }

    public void setSaldoAnterior(long saldoAnterior) {
        this.saldoAnterior = saldoAnterior;
    }

    public long getSaldoPosterior() {
        return saldoPosterior;
    }

    public void setSaldoPosterior(long saldoPosterior) {
        this.saldoPosterior = saldoPosterior;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getRealizadoPorEmail() {
        return realizadoPorEmail;
    }

    public void setRealizadoPorEmail(String realizadoPorEmail) {
        this.realizadoPorEmail = realizadoPorEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
