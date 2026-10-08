package com.assistente.empresarial.dto;

import com.assistente.empresarial.enuns.StatusConversa;
import java.util.List;
import java.util.UUID;

public class ChatResponseDTO {

    private UUID conversaId;
    private String resposta;
    private List<String> fontes;
    private boolean transbordoHumano;
    private StatusConversa status;

    public ChatResponseDTO() {
    }

    public ChatResponseDTO(UUID conversaId, String resposta, List<String> fontes, boolean transbordoHumano, StatusConversa status) {
        this.conversaId = conversaId;
        this.resposta = resposta;
        this.fontes = fontes;
        this.transbordoHumano = transbordoHumano;
        this.status = status;
    }

    public UUID getConversaId() {
        return conversaId;
    }

    public void setConversaId(UUID conversaId) {
        this.conversaId = conversaId;
    }

    public String getResposta() {
        return resposta;
    }

    public void setResposta(String resposta) {
        this.resposta = resposta;
    }

    public List<String> getFontes() {
        return fontes;
    }

    public void setFontes(List<String> fontes) {
        this.fontes = fontes;
    }

    public boolean isTransbordoHumano() {
        return transbordoHumano;
    }

    public void setTransbordoHumano(boolean transbordoHumano) {
        this.transbordoHumano = transbordoHumano;
    }

    public StatusConversa getStatus() {
        return status;
    }

    public void setStatus(StatusConversa status) {
        this.status = status;
    }
}
