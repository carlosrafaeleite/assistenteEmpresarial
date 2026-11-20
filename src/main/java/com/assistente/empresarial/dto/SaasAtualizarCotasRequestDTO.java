package com.assistente.empresarial.dto;

public class SaasAtualizarCotasRequestDTO {
    private String plano;
    private Integer limiteAssistentes;
    private Boolean status;
    private Boolean concederFranquiaPlano;
    private String motivo;

    public String getPlano() {
        return plano;
    }

    public void setPlano(String plano) {
        this.plano = plano;
    }

    public Integer getLimiteAssistentes() {
        return limiteAssistentes;
    }

    public void setLimiteAssistentes(Integer limiteAssistentes) {
        this.limiteAssistentes = limiteAssistentes;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getConcederFranquiaPlano() {
        return concederFranquiaPlano;
    }

    public void setConcederFranquiaPlano(Boolean concederFranquiaPlano) {
        this.concederFranquiaPlano = concederFranquiaPlano;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
