package com.assistente.empresarial.service.tool.impl;

import com.assistente.empresarial.service.tool.EnterpriseTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HorarioAtendimentoTool implements EnterpriseTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "consultar_horario_atendimento";
    }

    @Override
    public String getDescription() {
        return "Consulta o horário de funcionamento e canais de contato da empresa para um setor específico (suporte, vendas ou financeiro).";
    }

    @Override
    public ToolSpecification getSpecification() {
        Map<String, Map<String, Object>> properties = new HashMap<>();
        Map<String, Object> setorProp = new HashMap<>();
        setorProp.put("type", "string");
        setorProp.put("description", "Nome do setor desejado: 'suporte', 'vendas' ou 'financeiro'.");
        properties.put("setor", setorProp);

        ToolParameters params = ToolParameters.builder()
                .properties(properties)
                .required(List.of("setor"))
                .build();

        return ToolSpecification.builder()
                .name(getName())
                .description(getDescription())
                .parameters(params)
                .build();
    }

    @Override
    public String execute(String jsonArguments) {
        String setor = "suporte";
        try {
            if (jsonArguments != null && !jsonArguments.trim().isEmpty()) {
                JsonNode node = objectMapper.readTree(jsonArguments);
                if (node.has("setor")) {
                    setor = node.get("setor").asText().toLowerCase();
                }
            }
        } catch (Exception ignored) {
        }

        return switch (setor) {
            case "vendas", "comercial" -> "Horário Comercial: Segunda a Sexta, das 08h às 18h. E-mail: vendas@empresa.com | WhatsApp: (11) 98888-0001.";
            case "financeiro" -> "Horário Financeiro: Segunda a Sexta, das 09h às 17h. E-mail: financeiro@empresa.com.";
            default -> "Horário do Suporte Técnico: 24 horas por dia, 7 dias por semana via Chat e Tickets, com atendimento humano das 08h às 20h.";
        };
    }
}
