package com.assistente.empresarial.dto;

public class SaasCreditoOperacaoRequestDTO {
    private String operacao; // ADICIONAR, REMOVER, DEFINIR
    private long quantidadeTokens;
    private String motivo;

    public String getOperacao() {
        return operacao;
    }

    public void setOperacao(String operacao) {
        this.operacao = operacao;
    }

    public long getQuantidadeTokens() {
        return quantidadeTokens;
    }

    public void setQuantidadeTokens(long quantidadeTokens) {
        this.quantidadeTokens = quantidadeTokens;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
