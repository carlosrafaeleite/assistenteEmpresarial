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
public class PoliticaEmpresaTool implements EnterpriseTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "consultar_politicas_lgpd";
    }

    @Override
    public String getDescription() {
        return "Consulta as diretrizes de privacidade, segurança de dados e conformidade com a LGPD da organização.";
    }

    @Override
    public ToolSpecification getSpecification() {
        Map<String, Map<String, Object>> properties = new HashMap<>();

        Map<String, Object> topicoProp = new HashMap<>();
        topicoProp.put("type", "string");
        topicoProp.put("description", "Tópico de privacidade (ex: exclusão de dados, compartilhamento, cookies).");
        properties.put("topico", topicoProp);

        ToolParameters params = ToolParameters.builder()
                .properties(properties)
                .required(List.of("topico"))
                .build();

        return ToolSpecification.builder()
                .name(getName())
                .description(getDescription())
                .parameters(params)
                .build();
    }

    @Override
    public String execute(String jsonArguments) {
        return "Diretrizes de Privacidade & LGPD: Todos os dados dos clientes são criptografados em trânsito (TLS 1.3) e em repouso (AES-256). O titular pode solicitar a qualquer momento a visualização, portabilidade ou revogação/exclusão completa de seus dados pessoais pelo canal dpo@empresa.com.";
    }
}
