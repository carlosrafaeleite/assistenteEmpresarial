package com.assistente.empresarial.model;

import com.assistente.empresarial.enuns.TipoTransacaoCredito;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credito_transacoes")
public class CreditoTransacao {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "empresa_nome", nullable = false)
    private String empresaNome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoTransacaoCredito tipo;

    @Column(name = "quantidade_tokens", nullable = false)
    private long quantidadeTokens;

    @Column(name = "saldo_anterior", nullable = false)
    private long saldoAnterior;

    @Column(name = "saldo_posterior", nullable = false)
    private long saldoPosterior;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "realizado_por_email", nullable = false)
    private String realizadoPorEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CreditoTransacao() {
    }

    public CreditoTransacao(UUID empresaId, String empresaNome, TipoTransacaoCredito tipo,
                            long quantidadeTokens, long saldoAnterior, long saldoPosterior,
                            String motivo, String realizadoPorEmail) {
        this.empresaId = empresaId;
        this.empresaNome = empresaNome;
        this.tipo = tipo;
        this.quantidadeTokens = quantidadeTokens;
        this.saldoAnterior = saldoAnterior;
        this.saldoPosterior = saldoPosterior;
        this.motivo = motivo;
        this.realizadoPorEmail = realizadoPorEmail;
        this.createdAt = LocalDateTime.now();
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
