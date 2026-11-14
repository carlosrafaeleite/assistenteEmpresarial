package com.assistente.empresarial.service.streaming;

import com.assistente.empresarial.enuns.LlmProvider;
import com.assistente.empresarial.model.Conversa;
import com.assistente.empresarial.model.Mensagem;
import com.assistente.empresarial.repository.ConversaRepository;
import com.assistente.empresarial.repository.MensagemRepository;
import com.assistente.empresarial.service.llm.LlmRouterService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço de Streaming de Respostas (Server-Sent Events - SSE).
 * - Transmissão token-a-token em tempo real para o widget web e chat.
 * - Integração nativa com StreamingChatLanguageModel (OpenAI e Ollama).
 * - Encerramento ordenado e persistência do histórico completo ao final da transmissão.
 */
@Service
public class StreamingChatService {

    private static final Logger log = LoggerFactory.getLogger(StreamingChatService.class);

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.chat-model:gpt-4o-mini}")
    private String openAiChatModel;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.chat-model:llama3.2}")
    private String ollamaChatModel;

    private final LlmRouterService routerService;
    private final MensagemRepository mensagemRepository;
    private final ConversaRepository conversaRepository;

    public StreamingChatService(LlmRouterService routerService,
                                MensagemRepository mensagemRepository,
                                ConversaRepository conversaRepository) {
        this.routerService = routerService;
        this.mensagemRepository = mensagemRepository;
        this.conversaRepository = conversaRepository;
    }

    /**
     * Executa streaming de resposta da LLM diretamente no SseEmitter fornecido.
     */
    public void streamResponse(List<ChatMessage> messages,
                               Conversa conversa,
                               List<String> fontes,
                               SseEmitter emitter) {

        StreamingChatLanguageModel streamingModel = resolveStreamingModel();

        if (streamingModel == null) {
            log.warn("Nenhum modelo de streaming nativo configurado. Realizando streaming simulado por chunks.");
            streamSimulado(messages, conversa, fontes, emitter);
            return;
        }

        StringBuilder fullTextBuilder = new StringBuilder();

        // Envia evento inicial de fontes recuperadas no RAG
        if (fontes != null && !fontes.isEmpty()) {
            sendSseEvent(emitter, "fontes", StreamChunkDTO.fontes(Map.of("fontes", fontes)));
        }

        try {
            streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    fullTextBuilder.append(token);
                    sendSseEvent(emitter, "token", StreamChunkDTO.token(token));
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    String fullResponse = fullTextBuilder.toString().trim();
                    log.info("Streaming concluído com sucesso. Tamanho gerado: {} chars", fullResponse.length());

                    salvarRespostaIA(conversa, fullResponse, fontes);

                    sendSseEvent(emitter, "done", StreamChunkDTO.done(Map.of(
                            "conversaId", conversa.getId(),
                            "status", conversa.getStatus().name()
                    )));
                    emitter.complete();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Erro no streaming do modelo LLM: {}", error.getMessage(), error);
                    sendSseEvent(emitter, "error", StreamChunkDTO.error("Ocorreu uma instabilidade na transmissão."));
                    emitter.completeWithError(error);
                }
            });
        } catch (Exception e) {
            log.error("Falha ao iniciar streaming: {}", e.getMessage(), e);
            sendSseEvent(emitter, "error", StreamChunkDTO.error("Falha ao conectar com o serviço de IA."));
            emitter.completeWithError(e);
        }
    }

    private void streamSimulado(List<ChatMessage> messages, Conversa conversa, List<String> fontes, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> {
            try {
                if (fontes != null && !fontes.isEmpty()) {
                    sendSseEvent(emitter, "fontes", StreamChunkDTO.fontes(Map.of("fontes", fontes)));
                }

                Response<AiMessage> response = routerService.getActiveModel().generate(messages);
                String text = response.content().text();

                // Divide em pequenas palavras simulando fluidez rápida de streaming
                String[] words = text.split("(?<=\\s)");
                for (String word : words) {
                    sendSseEvent(emitter, "token", StreamChunkDTO.token(word));
                    Thread.sleep(20);
                }

                salvarRespostaIA(conversa, text, fontes);

                sendSseEvent(emitter, "done", StreamChunkDTO.done(Map.of(
                        "conversaId", conversa.getId(),
                        "status", conversa.getStatus().name()
                )));
                emitter.complete();
            } catch (Exception e) {
                log.error("Erro no streaming simulado: {}", e.getMessage(), e);
                sendSseEvent(emitter, "error", StreamChunkDTO.error(e.getMessage()));
                emitter.completeWithError(e);
            }
        });
    }

    private void salvarRespostaIA(Conversa conversa, String respostaTexto, List<String> fontes) {
        try {
            Mensagem msgAssistente = new Mensagem(conversa.getId(), "ASSISTENTE", respostaTexto);
            if (fontes != null && !fontes.isEmpty()) {
                msgAssistente.setFontes(String.join(", ", fontes));
            }
            mensagemRepository.save(msgAssistente);

            conversa.setUpdatedAt(LocalDateTime.now());
            conversaRepository.save(conversa);
        } catch (Exception e) {
            log.error("Erro ao salvar mensagem gerada por streaming no banco: {}", e.getMessage(), e);
        }
    }

    private StreamingChatLanguageModel resolveStreamingModel() {
        LlmProvider active = routerService.getActiveProvider();

        if (active == LlmProvider.OPENAI && openAiApiKey != null && !openAiApiKey.trim().isEmpty() && !openAiApiKey.equalsIgnoreCase("sua-chave-aqui")) {
            return OpenAiStreamingChatModel.builder()
                    .apiKey(openAiApiKey.trim())
                    .modelName(openAiChatModel)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }

        if (active == LlmProvider.OLLAMA && ollamaBaseUrl != null && !ollamaBaseUrl.trim().isEmpty()) {
            return OllamaStreamingChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaChatModel)
                    .timeout(Duration.ofSeconds(120))
                    .build();
        }

        return null;
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.warn("Falha de I/O no envio de evento SSE (cliente pode ter desconectado): {}", e.getMessage());
        }
    }
}
