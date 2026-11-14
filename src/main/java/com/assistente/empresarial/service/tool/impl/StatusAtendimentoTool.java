package com.assistente.empresarial.service.tool.impl;

import com.assistente.empresarial.service.tool.EnterpriseTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StatusAtendimentoTool implements EnterpriseTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "consultar_status_chamado";
    }

    @Override
    public String getDescription() {
        return "Consulta a situação atual de um chamado ou protocolo de suporte do cliente.";
    }

    @Override
    public ToolSpecification getSpecification() {
        Map<String, Map<String, Object>> properties = new HashMap<>();

        Map<String, Object> protocoloProp = new HashMap<>();
        protocoloProp.put("type", "string");
        protocoloProp.put("description", "Número do protocolo do chamado (ex: CH-1029).");
        properties.put("protocolo", protocoloProp);

        ToolParameters params = ToolParameters.builder()
                .properties(properties)
                .required(List.of("protocolo"))
                .build();

        return ToolSpecification.builder()
                .name(getName())
                .description(getDescription())
                .parameters(params)
                .build();
    }

    @Override
    public String execute(String jsonArguments) {
        String protocolo = "CH-0000";
        try {
            if (jsonArguments != null && !jsonArguments.trim().isEmpty()) {
                JsonNode node = objectMapper.readTree(jsonArguments);
                if (node.has("protocolo")) {
                    protocolo = node.get("protocolo").asText();
                }
            }
        } catch (Exception ignored) {
        }

        return String.format(
                "Status do Protocolo %s: EM_ANALISE pela equipe técnica de nível 2. Previsão de retorno em até 4 horas úteis.",
                protocolo
        );
    }
}
