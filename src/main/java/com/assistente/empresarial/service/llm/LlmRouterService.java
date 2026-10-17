package com.assistente.empresarial.service.llm;

import com.assistente.empresarial.enuns.LlmProvider;
import com.assistente.empresarial.exception.BusinessException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmRouterService {

    private static final Logger log = LoggerFactory.getLogger(LlmRouterService.class);

    @Value("${app.llm.provider:OPENAI}")
    private String defaultProviderProperty;

    private final OpenAiChatService openAiChatService;
    private final GeminiChatService geminiChatService;
    private final OllamaChatService ollamaChatService;

    private final Map<LlmProvider, LlmChatService> services = new EnumMap<>(LlmProvider.class);
    private volatile LlmProvider activeProvider;

    public LlmRouterService(OpenAiChatService openAiChatService,
                            GeminiChatService geminiChatService,
                            OllamaChatService ollamaChatService) {
        this.openAiChatService = openAiChatService;
        this.geminiChatService = geminiChatService;
        this.ollamaChatService = ollamaChatService;
    }

    @PostConstruct
    public void init() {
        services.put(LlmProvider.OPENAI, openAiChatService);
        services.put(LlmProvider.GEMINI, geminiChatService);
        services.put(LlmProvider.OLLAMA, ollamaChatService);

        this.activeProvider = LlmProvider.fromString(defaultProviderProperty);
        log.info("LlmRouterService iniciado com provedor preferencial: {}", this.activeProvider);
    }

    public LlmProvider getActiveProvider() {
        return activeProvider;
    }

    public void setActiveProvider(LlmProvider provider) {
        if (provider != null) {
            log.info("Alterando provedor ativo de LLM para: {}", provider);
            this.activeProvider = provider;
        }
    }

    public LlmChatService getService(LlmProvider provider) {
        LlmChatService service = services.get(provider);
        if (service == null) {
            throw new BusinessException("Provedor de LLM não suportado: " + provider);
        }
        return service;
    }

    public LlmChatService getActiveService() {
        LlmChatService preferredService = services.get(activeProvider);
        if (preferredService != null && preferredService.isConfigured()) {
            return preferredService;
        }

        log.warn("Provedor configurado ({}) não possui chave ou parâmetros válidos. Verificando fallbacks disponíveis...", activeProvider);
        for (LlmChatService candidate : services.values()) {
            if (candidate.isConfigured()) {
                log.info("Provedor alternativo selecionado: {} ({})", candidate.getProvider(), candidate.getModelName());
                return candidate;
            }
        }

        // Se nenhum estiver pronto, retorna o preferido para que lance a exceção detalhada com instruções
        return preferredService != null ? preferredService : openAiChatService;
    }

    public ChatLanguageModel getActiveModel() {
        return getActiveService().getModel();
    }

    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return getActiveModel().generate(messages);
    }

    public String generate(String systemPrompt, String userMessage) {
        return getActiveService().generate(systemPrompt, userMessage);
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("provedorAtivo", activeProvider.name());
        status.put("provedorEmUso", getActiveService().getProvider().name());
        status.put("modeloEmUso", getActiveService().getModelName());

        Map<String, Object> providersInfo = new java.util.LinkedHashMap<>();
        for (Map.Entry<LlmProvider, LlmChatService> entry : services.entrySet()) {
            Map<String, Object> info = new java.util.LinkedHashMap<>();
            info.put("modelo", entry.getValue().getModelName());
            info.put("configurado", entry.getValue().isConfigured());
            providersInfo.put(entry.getKey().name(), info);
        }
        status.put("provedores", providersInfo);
        return status;
    }
}
