package com.assistente.empresarial.service.fallback;

import com.assistente.empresarial.enuns.LlmProvider;
import com.assistente.empresarial.service.llm.GeminiChatService;
import com.assistente.empresarial.service.llm.LlmChatService;
import com.assistente.empresarial.service.llm.LlmRouterService;
import com.assistente.empresarial.service.llm.OllamaChatService;
import com.assistente.empresarial.service.llm.OpenAiChatService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de Resiliência e Fallback Multi-Provedor em Tempo de Execução.
 * - Cascata automática: Provedor Ativo -> Provedor Secundário -> Provedor Local (Ollama) -> Mensagem de Degradação Graciosa.
 * - Captura falhas de timeout, limite de taxa (HTTP 429) e erros 5xx dos provedores em tempo real.
 */
@Service
public class ResilientLlmService {

    private static final Logger log = LoggerFactory.getLogger(ResilientLlmService.class);

    private final LlmRouterService routerService;
    private final OpenAiChatService openAiChatService;
    private final GeminiChatService geminiChatService;
    private final OllamaChatService ollamaChatService;

    public ResilientLlmService(LlmRouterService routerService,
                               OpenAiChatService openAiChatService,
                               GeminiChatService geminiChatService,
                               OllamaChatService ollamaChatService) {
        this.routerService = routerService;
        this.openAiChatService = openAiChatService;
        this.geminiChatService = geminiChatService;
        this.ollamaChatService = ollamaChatService;
    }

    public FallbackResult generateWithFallback(List<ChatMessage> messages) {
        LlmProvider active = routerService.getActiveProvider();
        List<LlmChatService> cascadeOrder = determineCascadeOrder(active);

        String firstErrorMessage = null;
        boolean fallbackOccurred = false;

        for (int i = 0; i < cascadeOrder.size(); i++) {
            LlmChatService service = cascadeOrder.get(i);
            if (!service.isConfigured()) {
                continue;
            }

            try {
                log.debug("Tentando gerar resposta com provedor: {}", service.getProvider());
                Response<AiMessage> response = service.getModel().generate(messages);
                return new FallbackResult(response, service.getProvider(), fallbackOccurred, firstErrorMessage);
            } catch (Exception e) {
                log.warn("Falha no provedor {}: {}. Acionando fallback...", service.getProvider(), e.getMessage());
                if (firstErrorMessage == null) {
                    firstErrorMessage = e.getMessage();
                }
                fallbackOccurred = true;
            }
        }

        // Se todos os provedores externos falharem, resposta de contingência determinística graciosa
        log.error("Todos os provedores de LLM falharam. Ativando resposta de degradação graciosa.");
        String degradedText = "No momento nossos servidores de inteligência artificial estão com alta demanda temporária. Suas dúvidas foram registradas. Você gostaria de falar diretamente com um atendente humano?";
        AiMessage degradedMessage = AiMessage.from(degradedText);
        return new FallbackResult(Response.from(degradedMessage), LlmProvider.OLLAMA, true, firstErrorMessage);
    }

    private List<LlmChatService> determineCascadeOrder(LlmProvider activeProvider) {
        List<LlmChatService> order = new ArrayList<>();
        if (activeProvider == LlmProvider.OPENAI) {
            order.add(openAiChatService);
            order.add(geminiChatService);
            order.add(ollamaChatService);
        } else if (activeProvider == LlmProvider.GEMINI) {
            order.add(geminiChatService);
            order.add(openAiChatService);
            order.add(ollamaChatService);
        } else {
            order.add(ollamaChatService);
            order.add(openAiChatService);
            order.add(geminiChatService);
        }
        return order;
    }
}
