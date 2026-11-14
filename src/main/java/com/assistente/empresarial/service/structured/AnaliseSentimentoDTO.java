package com.assistente.empresarial.service.structured;

public class AnaliseSentimentoDTO {

    private String sentimento; // POSITIVO, NEUTRO, NEGATIVO
    private Integer pontuacao; // 1 a 10
    private Boolean clienteEmRisco;
    private String motivoChave;
    private String acaoRecomendada;

    public AnaliseSentimentoDTO() {
    }

    public String getSentimento() {
        return sentimento;
    }

    public void setSentimento(String sentimento) {
        this.sentimento = sentimento;
    }

    public Integer getPontuacao() {
        return pontuacao;
    }

    public void setPontuacao(Integer pontuacao) {
        this.pontuacao = pontuacao;
    }

    public Boolean getClienteEmRisco() {
        return clienteEmRisco;
    }

    public void setClienteEmRisco(Boolean clienteEmRisco) {
        this.clienteEmRisco = clienteEmRisco;
    }

    public String getMotivoChave() {
        return motivoChave;
    }

    public void setMotivoChave(String motivoChave) {
        this.motivoChave = motivoChave;
    }

    public String getAcaoRecomendada() {
        return acaoRecomendada;
    }

    public void setAcaoRecomendada(String acaoRecomendada) {
        this.acaoRecomendada = acaoRecomendada;
    }
}
