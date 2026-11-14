package com.assistente.empresarial.service.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ToolExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionEngine.class);

    private final EnterpriseToolRegistry registry;

    public ToolExecutionEngine(EnterpriseToolRegistry registry) {
        this.registry = registry;
    }

    public ToolExecutionResultMessage execute(ToolExecutionRequest request) {
        String toolName = request.name();
        String arguments = request.arguments();
        log.info("Executando ferramenta corporativa '{}' com argumentos: {}", toolName, arguments);

        long start = System.currentTimeMillis();
        String result;

        try {
            Optional<EnterpriseTool> toolOpt = registry.findByName(toolName);
            if (toolOpt.isPresent()) {
                result = toolOpt.get().execute(arguments);
            } else {
                result = "Erro: Ferramenta desconhecida '" + toolName + "'.";
                log.warn(result);
            }
        } catch (Exception e) {
            result = "Erro ao executar a ferramenta '" + toolName + "': " + e.getMessage();
            log.error("Exceção na execução da ferramenta: {}", e.getMessage(), e);
        }

        long duration = System.currentTimeMillis() - start;
        log.info("Ferramenta '{}' concluída em {}ms com resultado: {}", toolName, duration, result);

        return ToolExecutionResultMessage.from(request, result);
    }
}
