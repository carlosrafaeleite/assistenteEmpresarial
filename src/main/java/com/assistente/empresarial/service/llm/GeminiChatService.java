package com.assistente.empresarial.service.llm;

import com.assistente.empresarial.enuns.LlmProvider;
import com.assistente.empresarial.exception.BusinessException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class GeminiChatService implements LlmChatService {

    private static final Logger log = LoggerFactory.getLogger(GeminiChatService.class);

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.chat-model:gemini-1.5-flash}")
    private String chatModelName;

    @Value("${gemini.temperature:0.3}")
    private Double temperature;

    @Value("${gemini.timeout-seconds:60}")
    private Integer timeoutSeconds;

    private volatile ChatLanguageModel chatLanguageModel;

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.GEMINI;
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
            throw new BusinessException("A chave de API do Google Gemini (GEMINI_API_KEY) não está configurada no ambiente ou no application.properties.");
        }
        if (chatLanguageModel == null) {
            synchronized (this) {
                if (chatLanguageModel == null) {
                    log.info("Inicializando modelo Google Gemini: {} (temperatura: {}, timeout: {}s)",
                            chatModelName, temperature, timeoutSeconds);
                    chatLanguageModel = GoogleAiGeminiChatModel.builder()
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
