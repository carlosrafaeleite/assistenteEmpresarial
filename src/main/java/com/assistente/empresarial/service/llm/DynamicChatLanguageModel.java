package com.assistente.empresarial.service.llm;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

/**
 * Proxy dinâmico para ChatLanguageModel do LangChain4j.
 * Encaminha chamadas ao modelo ativo gerenciado pelo LlmRouterService.
 */
public class DynamicChatLanguageModel implements ChatLanguageModel {

    private final LlmRouterService routerService;

    public DynamicChatLanguageModel(LlmRouterService routerService) {
        this.routerService = routerService;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return routerService.getActiveModel().generate(messages);
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        return routerService.getActiveModel().generate(messages, toolSpecifications);
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
        return routerService.getActiveModel().generate(messages, toolSpecification);
    }
}
