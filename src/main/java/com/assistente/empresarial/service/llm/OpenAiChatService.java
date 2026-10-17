package com.assistente.empresarial.service.llm;

import com.assistente.empresarial.enuns.LlmProvider;
import com.assistente.empresarial.exception.BusinessException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class OpenAiChatService implements LlmChatService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatService.class);

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.chat-model:gpt-4o-mini}")
    private String chatModelName;

    @Value("${openai.temperature:0.3}")
    private Double temperature;

    @Value("${openai.timeout-seconds:60}")
    private Integer timeoutSeconds;

    private volatile ChatLanguageModel chatLanguageModel;

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.OPENAI;
    }

    @Override
    public String getModelName() {
        return chatModelName;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equalsIgnoreCase("sua-chave-aqui");
    }

    @Override
    public ChatLanguageModel getModel() {
        if (!isConfigured()) {
            throw new BusinessException("A chave de API da OpenAI (OPENAI_API_KEY) não está configurada no ambiente ou no application.properties.");
        }
        if (chatLanguageModel == null) {
            synchronized (this) {
                if (chatLanguageModel == null) {
                    log.info("Inicializando modelo OpenAI: {} (temperatura: {}, timeout: {}s)",
                            chatModelName, temperature, timeoutSeconds);
                    chatLanguageModel = OpenAiChatModel.builder()
                            .apiKey(apiKey.trim())
                            .modelName(chatModelName)
                            .temperature(temperature != null ? temperature : 0.3)
                            .timeout(Duration.ofSeconds(timeoutSeconds != null ? timeoutSeconds : 60))
                            .build();
                }
            }
        }
        return chatLanguageModel;
    }
}
