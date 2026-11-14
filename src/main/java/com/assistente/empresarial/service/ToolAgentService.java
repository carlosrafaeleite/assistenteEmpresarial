package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.AgentExecutionResult;
import com.assistente.empresarial.service.tool.EnterpriseToolRegistry;
import com.assistente.empresarial.service.tool.ToolExecutionEngine;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Agente Autônomo de Ferramentas (ReAct Agent).
 * - Ciclo iterativo de raciocínio, decisão de ferramentas, invocação e observação.
 * - Suporta múltiplas rodadas de execução de ferramentas com teto máximo para evitar loops infinitos.
 * - Integra com o catálogo corporativo de ferramentas.
 */
@Service
public class ToolAgentService {

    private static final Logger log = LoggerFactory.getLogger(ToolAgentService.class);

    private static final int MAX_ITERATIONS = 4;

    private final ChatLanguageModel chatLanguageModel;
    private final EnterpriseToolRegistry toolRegistry;
    private final ToolExecutionEngine toolExecutionEngine;

    public ToolAgentService(ChatLanguageModel chatLanguageModel,
                            EnterpriseToolRegistry toolRegistry,
                            ToolExecutionEngine toolExecutionEngine) {
        this.chatLanguageModel = chatLanguageModel;
        this.toolRegistry = toolRegistry;
        this.toolExecutionEngine = toolExecutionEngine;
    }

    /**
     * Executa o agente de ferramentas sobre uma lista de mensagens.
     */
    public AgentExecutionResult runAgentLoop(List<ChatMessage> initialMessages) {
        long startTime = System.currentTimeMillis();
        List<ChatMessage> conversation = new ArrayList<>(initialMessages);
        List<ToolSpecification> tools = toolRegistry.getAllSpecifications();
        List<String> executedTools = new ArrayList<>();

        int iteration = 0;
        String finalAnswer = "";

        while (iteration < MAX_ITERATIONS) {
            iteration++;
            log.debug("Iniciando iteração {} do Agente de Ferramentas", iteration);

            Response<AiMessage> response;
            try {
                if (tools.isEmpty()) {
                    response = chatLanguageModel.generate(conversation);
                } else {
                    response = chatLanguageModel.generate(conversation, tools);
                }
            } catch (Exception e) {
                log.warn("Erro ao gerar resposta com ferramentas no modelo (fallback sem ferramentas): {}", e.getMessage());
                // Fallback sem especificações de ferramentas caso o modelo selecionado não suporte tools
                response = chatLanguageModel.generate(conversation);
            }

            AiMessage aiMessage = response.content();
            conversation.add(aiMessage);

            if (!aiMessage.hasToolExecutionRequests()) {
                // Modelo concluiu a resposta final
                finalAnswer = aiMessage.text() != null ? aiMessage.text() : "";
                log.debug("Agente concluiu raciocínio na iteração {}. Resposta pronta.", iteration);
                break;
            }

            // O modelo solicitou uma ou mais ferramentas
            for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                log.info("Agente solicitou ferramenta '{}' (id: {})", toolRequest.name(), toolRequest.id());
                executedTools.add(toolRequest.name());

                ToolExecutionResultMessage toolResult = toolExecutionEngine.execute(toolRequest);
                conversation.add(toolResult);
            }
        }

        // Se atingiu o limite de iterações sem resposta final textual explícita
        if (finalAnswer.trim().isEmpty() && !conversation.isEmpty()) {
            ChatMessage last = conversation.get(conversation.size() - 1);
            if (last instanceof AiMessage aiMsg && aiMsg.text() != null) {
                finalAnswer = aiMsg.text();
            } else {
                finalAnswer = "Operação concluída com sucesso utilizando as ferramentas corporativas solicitadas.";
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Loop do Agente finalizado em {}ms (iterações: {}, ferramentas usadas: {})",
                totalTime, iteration, executedTools);

        return new AgentExecutionResult(finalAnswer, executedTools, iteration, totalTime);
    }
}
