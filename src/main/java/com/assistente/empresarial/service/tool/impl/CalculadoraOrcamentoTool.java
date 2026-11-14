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
public class CalculadoraOrcamentoTool implements EnterpriseTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "calcular_orcamento_planos";
    }

    @Override
    public String getDescription() {
        return "Calcula estimativa de custo mensal ou anual de acordo com o plano (BASIC, PRO ou ENTERPRISE) e a quantidade de usuários.";
    }

    @Override
    public ToolSpecification getSpecification() {
        Map<String, Map<String, Object>> properties = new HashMap<>();

        Map<String, Object> planoProp = new HashMap<>();
        planoProp.put("type", "string");
        planoProp.put("description", "Nome do plano: 'BASIC', 'PRO' ou 'ENTERPRISE'.");
        properties.put("plano", planoProp);

        Map<String, Object> usuariosProp = new HashMap<>();
        usuariosProp.put("type", "integer");
        usuariosProp.put("description", "Quantidade de usuários licensiados.");
        properties.put("usuarios", usuariosProp);

        ToolParameters params = ToolParameters.builder()
                .properties(properties)
                .required(List.of("plano", "usuarios"))
                .build();

        return ToolSpecification.builder()
                .name(getName())
                .description(getDescription())
                .parameters(params)
                .build();
    }

    @Override
    public String execute(String jsonArguments) {
        String plano = "PRO";
        int usuarios = 1;

        try {
            if (jsonArguments != null && !jsonArguments.trim().isEmpty()) {
                JsonNode node = objectMapper.readTree(jsonArguments);
                if (node.has("plano")) plano = node.get("plano").asText().toUpperCase();
                if (node.has("usuarios")) usuarios = Math.max(1, node.get("usuarios").asInt());
            }
        } catch (Exception ignored) {
        }

        double precoBasePorUsuario;
        switch (plano) {
            case "BASIC" -> precoBasePorUsuario = 49.90;
            case "ENTERPRISE" -> precoBasePorUsuario = 149.90;
            default -> {
                plano = "PRO";
                precoBasePorUsuario = 89.90;
            }
        }

        double valorMensal = usuarios * precoBasePorUsuario;
        double desconto = usuarios >= 10 ? 0.15 : (usuarios >= 5 ? 0.10 : 0.0);
        double valorFinal = valorMensal * (1.0 - desconto);

        return String.format(
                "Simulação de Orçamento: Plano %s para %d usuário(s). Preço de tabela: R$ %.2f/mês. Desconto de volume: %.0f%%. Valor final: R$ %.2f/mês.",
                plano, usuarios, valorMensal, desconto * 100, valorFinal
        );
    }
}
