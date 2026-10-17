package com.assistente.empresarial.service.llm;

import com.assistente.empresarial.enuns.LlmProvider;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

public interface LlmChatService {

    LlmProvider getProvider();

    String getModelName();

    boolean isConfigured();

    ChatLanguageModel getModel();

    default Response<AiMessage> generate(List<ChatMessage> messages) {
        return getModel().generate(messages);
    }

    default String generate(String systemPrompt, String userMessage) {
        Response<AiMessage> response = getModel().generate(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userMessage)
        );
        return response.content().text();
    }
}
