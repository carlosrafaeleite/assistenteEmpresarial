package com.assistente.empresarial.service.structured;

import java.util.List;

public class TicketSuporteDTO {

    private String categoria; // TECNICO, FINANCEIRO, COMERCIAL, DUVIDA
    private String urgencia; // ALTA, MEDIA, BAIXA
    private String titulo;
    private String descricaoProblema;
    private List<String> acoesSugeridas;

    public TicketSuporteDTO() {
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getUrgencia() {
        return urgencia;
    }

    public void setUrgencia(String urgencia) {
        this.urgencia = urgencia;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricaoProblema() {
        return descricaoProblema;
    }

    public void setDescricaoProblema(String descricaoProblema) {
        this.descricaoProblema = descricaoProblema;
    }

    public List<String> getAcoesSugeridas() {
        return acoesSugeridas;
    }

    public void setAcoesSugeridas(List<String> acoesSugeridas) {
        this.acoesSugeridas = acoesSugeridas;
    }
}
