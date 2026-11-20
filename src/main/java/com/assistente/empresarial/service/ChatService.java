package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.AgentExecutionResult;
import com.assistente.empresarial.dto.CachedPromptResponse;
import com.assistente.empresarial.dto.ChatRequestDTO;
import com.assistente.empresarial.dto.ChatResponseDTO;
import com.assistente.empresarial.dto.FallbackResult;
import com.assistente.empresarial.dto.GuardrailResult;
import com.assistente.empresarial.dto.RagRetrievalResult;
import com.assistente.empresarial.dto.TransbordoEventDTO;
import com.assistente.empresarial.dto.WidgetConfigResponseDTO;
import com.assistente.empresarial.enuns.StatusConversa;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.ResourceNotFoundException;
import com.assistente.empresarial.model.*;
import com.assistente.empresarial.repository.AssistenteRepository;
import com.assistente.empresarial.repository.ConversaRepository;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.repository.MensagemRepository;
import com.assistente.empresarial.repository.VectorStoreRepository;
import com.assistente.empresarial.security.TenantContext;
import com.assistente.empresarial.service.tool.EnterpriseToolRegistry;
import com.assistente.empresarial.service.tool.ToolExecutionEngine;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final EmpresaRepository empresaRepository;
    private final AssistenteRepository assistenteRepository;
    private final ConversaRepository conversaRepository;
    private final MensagemRepository mensagemRepository;
    private final ChatLanguageModel chatLanguageModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final PasswordEncoder passwordEncoder;

    // Componentes de Arquitetura Robusta e Híbrida (Ollama RAG + Gemini Chat)
    private final GuardrailsService guardrailsService;
    private final PromptCacheService promptCacheService;
    private final ContextWindowService contextWindowService;
    private final AdvancedRagService advancedRagService;
    private final VectorStoreRepository vectorStoreRepository;
    private final ToolAgentService toolAgentService;
    private final EnterpriseToolRegistry toolRegistry;
    private final ResilientLlmService resilientLlmService;
    private final AiObservabilityService observabilityService;
    private final StreamingChatService streamingChatService;
    private final StructuredOutputService structuredOutputService;
    private final RateLimitService rateLimitService;
    private final HumanHandoffService humanHandoffService;
    private final SlackWebhookService slackWebhookService;

    /**
     * Construtor principal gerenciado pelo Spring com injeção de todos os pilares da arquitetura.
     */
    @Autowired
    public ChatService(EmpresaRepository empresaRepository,
                       AssistenteRepository assistenteRepository,
                       ConversaRepository conversaRepository,
                       MensagemRepository mensagemRepository,
                       ChatLanguageModel chatLanguageModel,
                       EmbeddingModel embeddingModel,
                       EmbeddingStore<TextSegment> embeddingStore,
                       PasswordEncoder passwordEncoder,
                       GuardrailsService guardrailsService,
                       PromptCacheService promptCacheService,
                       ContextWindowService contextWindowService,
                       AdvancedRagService advancedRagService,
                       VectorStoreRepository vectorStoreRepository,
                       ToolAgentService toolAgentService,
                       EnterpriseToolRegistry toolRegistry,
                       ResilientLlmService resilientLlmService,
                       AiObservabilityService observabilityService,
                       StreamingChatService streamingChatService,
                       StructuredOutputService structuredOutputService,
                       RateLimitService rateLimitService,
                       HumanHandoffService humanHandoffService,
                       @Autowired(required = false) SlackWebhookService slackWebhookService) {
        this.empresaRepository = empresaRepository;
        this.assistenteRepository = assistenteRepository;
        this.conversaRepository = conversaRepository;
        this.mensagemRepository = mensagemRepository;
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.passwordEncoder = passwordEncoder;
        this.guardrailsService = guardrailsService;
        this.promptCacheService = promptCacheService;
        this.contextWindowService = contextWindowService;
        this.advancedRagService = advancedRagService;
        this.vectorStoreRepository = vectorStoreRepository;
        this.toolAgentService = toolAgentService;
        this.toolRegistry = toolRegistry;
        this.resilientLlmService = resilientLlmService;
        this.observabilityService = observabilityService;
        this.streamingChatService = streamingChatService;
        this.structuredOutputService = structuredOutputService;
        this.rateLimitService = rateLimitService != null ? rateLimitService : new RateLimitService();
        this.humanHandoffService = humanHandoffService != null ? humanHandoffService : new HumanHandoffService();
        this.slackWebhookService = slackWebhookService != null ? slackWebhookService : new SlackWebhookService();
    }

    /**
     * Construtor de compatibilidade para testes unitários legados.
     */
    public ChatService(EmpresaRepository empresaRepository,
                       AssistenteRepository assistenteRepository,
                       ConversaRepository conversaRepository,
                       MensagemRepository mensagemRepository,
                       ChatLanguageModel chatLanguageModel,
                       EmbeddingModel embeddingModel,
                       EmbeddingStore<TextSegment> embeddingStore,
                       PasswordEncoder passwordEncoder) {
        this.empresaRepository = empresaRepository;
        this.assistenteRepository = assistenteRepository;
        this.conversaRepository = conversaRepository;
        this.mensagemRepository = mensagemRepository;
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.passwordEncoder = passwordEncoder;

        this.guardrailsService = new GuardrailsService();
        this.promptCacheService = new PromptCacheService();
        this.contextWindowService = new ContextWindowService(mensagemRepository);
        this.vectorStoreRepository = new VectorStoreRepository(embeddingStore, null);
        this.advancedRagService = new AdvancedRagService(embeddingModel, vectorStoreRepository);
        this.toolRegistry = new EnterpriseToolRegistry(Collections.emptyList());
        ToolExecutionEngine toolEngine = new ToolExecutionEngine(toolRegistry);
        this.toolAgentService = new ToolAgentService(chatLanguageModel, toolRegistry, toolEngine);
        this.resilientLlmService = null;
        this.observabilityService = new AiObservabilityService();
        this.streamingChatService = null;
        this.structuredOutputService = null;
        this.rateLimitService = new RateLimitService();
        this.humanHandoffService = new HumanHandoffService();
        this.slackWebhookService = new SlackWebhookService();
    }

    public WidgetConfigResponseDTO obterConfiguracaoWidget(String slugEmpresa, String slugAssistente) {
        Empresa empresa = empresaRepository.findBySlug(slugEmpresa)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com slug: " + slugEmpresa));

        Assistente assistente = assistenteRepository.findBySlugAndEmpresaId(slugAssistente, empresa.getId())
                .orElseGet(() -> buscarAssistentePadrao(empresa.getId()));

        WidgetConfigResponseDTO dto = new WidgetConfigResponseDTO();
        dto.setEmpresaNome(empresa.getNome());
        dto.setEmpresaSlug(empresa.getSlug());
        dto.setAssistenteId(assistente.getId());

        String nomeBotReal = (empresa.getNomeBot() != null && !empresa.getNomeBot().trim().isEmpty())
                ? empresa.getNomeBot()
                : assistente.getNome();

        dto.setAssistenteNome(nomeBotReal);
        dto.setCorPrimaria(assistente.getCorPrimaria());
        dto.setCorSecundaria(assistente.getCorSecundaria());
        dto.setAvatarUrl(assistente.getAvatarUrl());
        dto.setMensagemBoasVindas(assistente.getMensagemBoasVindas());
        dto.setTomVoz(assistente.getTomVoz());
        dto.setProtegidoPorSenha(assistente.temSenha());
        dto.setModoIdentificacao(assistente.getModoIdentificacao());

        return dto;
    }

    public boolean validarSenhaAssistente(String slugEmpresa, String slugAssistente, String senhaDigitada) {
        Empresa empresa = empresaRepository.findBySlug(slugEmpresa)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com slug: " + slugEmpresa));

        Assistente assistente = assistenteRepository.findBySlugAndEmpresaId(slugAssistente, empresa.getId())
                .orElseGet(() -> buscarAssistentePadrao(empresa.getId()));

        if (!assistente.temSenha()) {
            return true;
        }

        if (senhaDigitada == null || senhaDigitada.trim().isEmpty()) {
            return false;
        }

        return passwordEncoder.matches(senhaDigitada.trim(), assistente.getSenha());
    }

    @Transactional
    public ChatResponseDTO responderPublico(String slugEmpresa, String slugAssistente, ChatRequestDTO request) {
        Empresa empresa = empresaRepository.findBySlug(slugEmpresa)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com slug: " + slugEmpresa));

        Assistente assistente = assistenteRepository.findBySlugAndEmpresaId(slugAssistente, empresa.getId())
                .orElseGet(() -> buscarAssistentePadrao(empresa.getId()));

        if (assistente.temSenha()) {
            String senhaAcesso = request.getSenhaAcesso();
            if (senhaAcesso == null || !passwordEncoder.matches(senhaAcesso.trim(), assistente.getSenha())) {
                throw new BadCredentialsException("Senha incorreta ou acesso não autorizado para este assistente.");
            }
        }

        // Validação de Limite de Caracteres por Pergunta e Rate Limiting (Perguntas por Minuto)
        String chaveCliente = extrairChaveRateLimit(slugEmpresa, request);
        rateLimitService.validarPerguntaERateLimit(chaveCliente, request.getMensagem());

        Conversa conversa = obterOuCriarConversa(request, empresa.getId(), assistente.getId(), "WIDGET");
        return executarProcessamentoChat(empresa, assistente, conversa, request.getMensagem());
    }

    @Transactional
    public ChatResponseDTO responderInterno(ChatRequestDTO request) {
        UUID empresaId = TenantContext.getTenantId();
        if (empresaId == null) {
            throw new BusinessException("Contexto de empresa não identificado.");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));

        rateLimitService.validarTamanhoPergunta(request.getMensagem());

        Assistente assistente = (request.getAssistenteId() != null)
                ? assistenteRepository.findByIdAndEmpresaId(request.getAssistenteId(), empresa.getId())
                .orElseGet(() -> buscarAssistentePadrao(empresa.getId()))
                : buscarAssistentePadrao(empresa.getId());

        Conversa conversa = obterOuCriarConversa(request, empresa.getId(), assistente.getId(), "INTERNO");
        return executarProcessamentoChat(empresa, assistente, conversa, request.getMensagem());
    }

    /**
     * Pipeline Híbrido de Execução de Chat:
     * 1. Guardrails e Sanitização
     * 2. Transbordo Humano em Tempo Real (SSE)
     * 3. Prompt Caching (Memória L1 + PostgreSQL L2)
     * 4. Busca Vetorial via Ollama (nomic-embed-text, 768d) + Recuperação pgvector isolada por empresa_id
     * 5. Geração de Resposta amigável via Gemini API (com Fallback resiliente)
     * 6. Desconto de créditos/tokens EXCLUSIVAMENTE nesta etapa de conversação no chat
     */
    private ChatResponseDTO executarProcessamentoChat(Empresa empresa, Assistente assistente, Conversa conversa, String mensagemUsuario) {
        long startTime = System.currentTimeMillis();
        ChatTrace trace = new ChatTrace();
        trace.setEmpresaId(empresa.getId());
        trace.setAssistenteId(assistente.getId());
        trace.setCanal(conversa.getCanal());

        // 1. Guardrails de Entrada: Detecção de Prompt Injection e Ofuscação de PII
        long tGuardrail = System.currentTimeMillis();
        GuardrailResult guardrailResult = guardrailsService.validateInput(mensagemUsuario);
        trace.setGuardrailTimeMs(System.currentTimeMillis() - tGuardrail);

        if (!guardrailResult.isAllowed()) {
            trace.setGuardrailViolations(guardrailResult.getViolations());
            trace.setTotalLatencyMs(System.currentTimeMillis() - startTime);
            observabilityService.recordTrace(trace);

            Mensagem msgUser = new Mensagem(conversa.getId(), "USUARIO", mensagemUsuario);
            mensagemRepository.save(msgUser);

            String respostaBloqueio = guardrailResult.getSanitizedText();
            Mensagem msgBot = new Mensagem(conversa.getId(), "ASSISTENTE", respostaBloqueio);
            mensagemRepository.save(msgBot);

            return new ChatResponseDTO(conversa.getId(), respostaBloqueio, Collections.emptyList(), false, conversa.getStatus());
        }

        String mensagemSanitizada = guardrailResult.getSanitizedText();

        // Salvar mensagem sanitizada do usuário
        Mensagem msgUser = new Mensagem(conversa.getId(), "USUARIO", mensagemSanitizada);
        mensagemRepository.save(msgUser);

        // 2. Se a conversa já está em Transbordo Humano, encaminha em tempo real via SSE ao atendente sem chamar a IA
        if (conversa.getStatus() == StatusConversa.EM_ATENDIMENTO_HUMANO
                || conversa.getStatus() == StatusConversa.AGUARDANDO_HUMANO) {
            conversa.setUpdatedAt(LocalDateTime.now());
            conversaRepository.save(conversa);

            humanHandoffService.notificarEvento(new TransbordoEventDTO(
                    "NOVA_MENSAGEM_CLIENTE",
                    conversa.getId(),
                    empresa.getId(),
                    conversa.getClienteNome(),
                    conversa.getStatus(),
                    mensagemSanitizada,
                    "USUARIO"
            ));

            String msgAguardo = conversa.getStatus() == StatusConversa.EM_ATENDIMENTO_HUMANO
                    ? "Mensagem enviada ao atendente. Em instantes ele responderá por aqui."
                    : "Sua solicitação já está na fila de atendimento humano. Um especialista irá atendê-lo em instantes.";
            return new ChatResponseDTO(conversa.getId(), msgAguardo, Collections.emptyList(), true, conversa.getStatus());
        }

        // 3. Verificação de nova solicitação de transbordo para Atendimento Humano
        if (checarSolicitacaoHumano(mensagemSanitizada)) {
            conversa.setStatus(StatusConversa.AGUARDANDO_HUMANO);
            conversa.setUpdatedAt(LocalDateTime.now());
            conversaRepository.save(conversa);

            String respostaHumano = "Entendido! Estou transferindo seu atendimento para a nossa equipe de suporte humano. Por favor, aguarde um momento.";
            Mensagem msgBot = new Mensagem(conversa.getId(), "ASSISTENTE", respostaHumano);
            mensagemRepository.save(msgBot);

            humanHandoffService.notificarEvento(new TransbordoEventDTO(
                    "SOLICITACAO_HUMANO",
                    conversa.getId(),
                    empresa.getId(),
                    conversa.getClienteNome(),
                    StatusConversa.AGUARDANDO_HUMANO,
                    mensagemSanitizada,
                    "USUARIO"
            ));

            // Dispara alerta assíncrono para o Incoming Webhook do Slack (se configurado pela empresa)
            slackWebhookService.notificarTransbordoAssincrono(empresa, conversa, mensagemSanitizada);

            trace.setTotalLatencyMs(System.currentTimeMillis() - startTime);
            observabilityService.recordTrace(trace);

            return new ChatResponseDTO(conversa.getId(), respostaHumano, Collections.emptyList(), true, StatusConversa.AGUARDANDO_HUMANO);
        }

        // 4. Prompt Caching: Consulta em memória L1 / Banco L2 antes de acionar RAG ou LLM
        long tCache = System.currentTimeMillis();
        CachedPromptResponse cached = promptCacheService.get(empresa.getId(), assistente.getId(), assistente.getPromptSistema(), mensagemSanitizada);
        trace.setCacheCheckTimeMs(System.currentTimeMillis() - tCache);

        if (cached != null) {
            trace.setCacheHit(true);
            trace.setTotalLatencyMs(System.currentTimeMillis() - startTime);
            observabilityService.recordTrace(trace);

            Mensagem msgAssistente = new Mensagem(conversa.getId(), "ASSISTENTE", cached.getResposta());
            if (!cached.getFontes().isEmpty()) {
                msgAssistente.setFontes(String.join(", ", cached.getFontes()));
            }
            mensagemRepository.save(msgAssistente);

            return new ChatResponseDTO(conversa.getId(), cached.getResposta(), cached.getFontes(), false, conversa.getStatus());
        }

        // 5. Verificar saldo de créditos/tokens da conta do lojista antes de acionar a geração da IA no chat
        if (empresa.getCreditosTokens() <= 0) {
            throw new BusinessException("Os créditos de tokens da empresa estão esgotados. Recarregue seus créditos ou atualize seu plano para continuar utilizando o chat com IA.");
        }

        // 6. Busca Vetorial Local (Ollama nomic-embed-text 768d) + Recuperação de Contexto no pgvector (isolado por empresa_id)
        long tRag = System.currentTimeMillis();
        RagRetrievalResult ragResult = advancedRagService.retrieveRelevantContext(empresa.getId(), assistente.getId(), mensagemSanitizada);
        trace.setRagRetrievalTimeMs(System.currentTimeMillis() - tRag);

        String contextoRecuperado = ragResult.getContextText();
        List<String> fontes = ragResult.getSources();

        // 7. Janela de Contexto: Juntar o contexto recuperado com a pergunta do usuário em um prompt estruturado
        String promptSistema = montarPromptSistema(empresa, assistente, contextoRecuperado);
        List<ChatMessage> windowedMessages = contextWindowService.buildWindowedMessages(conversa.getId(), promptSistema, mensagemSanitizada);

        int inputEstimatedTokens = contextWindowService.estimateTokens(promptSistema) + contextWindowService.estimateTokens(mensagemSanitizada);
        trace.setInputTokens(inputEstimatedTokens);

        // 8. Geração de Resposta (Gemini API / Agente de Ferramentas & Fallback de Resiliência)
        long tLlm = System.currentTimeMillis();
        String respostaBruta;
        try {
            AgentExecutionResult agentResult = toolAgentService.runAgentLoop(windowedMessages);
            respostaBruta = agentResult.getFinalResponse();
            trace.setToolsUsed(agentResult.getExecutedTools());
        } catch (Exception e) {
            log.warn("Exceção no Agente de Ferramentas: {}. Acionando Fallback de Resiliência Multi-Provedor...", e.getMessage());
            if (resilientLlmService != null) {
                FallbackResult fallbackResult = resilientLlmService.generateWithFallback(windowedMessages);
                respostaBruta = fallbackResult.getResponse().content().text();
                trace.setFallbackTriggered(fallbackResult.isFallbackTriggered());
                trace.setProviderUsed(fallbackResult.getProviderUsed());
            } else {
                respostaBruta = "Desculpe, estamos enfrentando uma instabilidade temporária no processamento de respostas. Você gostaria de falar com um atendente humano?";
            }
        }
        trace.setLlmInferenceTimeMs(System.currentTimeMillis() - tLlm);

        // 9. Guardrails de Saída: Sanitização contra vazamento de prompt de sistema ou segredos
        String respostaFinal = guardrailsService.validateAndSanitizeOutput(respostaBruta, promptSistema);
        int outputEstimatedTokens = contextWindowService.estimateTokens(respostaFinal);
        trace.setOutputTokens(outputEstimatedTokens);
        int totalTokensConversa = Math.max(1, trace.getInputTokens() + outputEstimatedTokens);
        trace.setTotalTokens(totalTokensConversa);

        // 10. REGRA DE NEGÓCIO: Descontar créditos/tokens EXCLUSIVAMENTE nesta etapa de conversação no chat
        empresa.debitarTokensChat(totalTokensConsumidos(totalTokensConversa));
        empresaRepository.save(empresa);

        // 11. Gravação no Prompt Cache (L1 + L2) para acelerar consultas futuras idênticas
        promptCacheService.put(empresa.getId(), assistente.getId(), assistente.getPromptSistema(), mensagemSanitizada, respostaFinal, fontes);

        // 12. Persistência da resposta do assistente
        Mensagem msgAssistente = new Mensagem(conversa.getId(), "ASSISTENTE", respostaFinal);
        if (!fontes.isEmpty()) {
            msgAssistente.setFontes(String.join(", ", fontes));
        }
        mensagemRepository.save(msgAssistente);

        conversa.setUpdatedAt(LocalDateTime.now());
        conversaRepository.save(conversa);

        // 13. Observabilidade: Registro persistente do trace da requisição
        trace.setTotalLatencyMs(System.currentTimeMillis() - startTime);
        observabilityService.recordTrace(trace);

        return new ChatResponseDTO(conversa.getId(), respostaFinal, fontes, false, conversa.getStatus());
    }

    private long totalTokensConsumidos(int tokensCalculados) {
        return Math.max(1L, tokensCalculados);
    }

    /**
     * Endpoint de Streaming em tempo real (Server-Sent Events) para widgets e aplicações conectadas.
     */
    public SseEmitter iniciarStreamingPublico(String slugEmpresa, String slugAssistente, ChatRequestDTO request) {
        Empresa empresa = empresaRepository.findBySlug(slugEmpresa)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com slug: " + slugEmpresa));

        Assistente assistente = assistenteRepository.findBySlugAndEmpresaId(slugAssistente, empresa.getId())
                .orElseGet(() -> buscarAssistentePadrao(empresa.getId()));

        if (assistente.temSenha()) {
            String senhaAcesso = request.getSenhaAcesso();
            if (senhaAcesso == null || !passwordEncoder.matches(senhaAcesso.trim(), assistente.getSenha())) {
                throw new BadCredentialsException("Senha incorreta ou acesso não autorizado.");
            }
        }

        String chaveCliente = extrairChaveRateLimit(slugEmpresa, request);
        rateLimitService.validarPerguntaERateLimit(chaveCliente, request.getMensagem());

        Conversa conversa = obterOuCriarConversa(request, empresa.getId(), assistente.getId(), "WIDGET_STREAM");

        GuardrailResult guardrailResult = guardrailsService.validateInput(request.getMensagem());
        String msgSanitizada = guardrailResult.getSanitizedText();

        Mensagem msgUser = new Mensagem(conversa.getId(), "USUARIO", msgSanitizada);
        mensagemRepository.save(msgUser);

        SseEmitter emitter = new SseEmitter(180_000L);

        // Se já está em atendimento humano ou solicitou humano, envia evento SSE e aciona Slack sem gastar tokens
        if (conversa.getStatus() == StatusConversa.EM_ATENDIMENTO_HUMANO
                || conversa.getStatus() == StatusConversa.AGUARDANDO_HUMANO
                || checarSolicitacaoHumano(msgSanitizada)) {

            boolean novaSolicitacao = conversa.getStatus() == StatusConversa.BOT;
            if (novaSolicitacao) {
                conversa.setStatus(StatusConversa.AGUARDANDO_HUMANO);
            }
            conversa.setUpdatedAt(LocalDateTime.now());
            conversaRepository.save(conversa);

            String textoAviso = novaSolicitacao
                    ? "Entendido! Estou transferindo seu atendimento para a nossa equipe de suporte humano. Por favor, aguarde um momento."
                    : (conversa.getStatus() == StatusConversa.EM_ATENDIMENTO_HUMANO
                        ? "Mensagem enviada ao atendente. Em instantes ele responderá por aqui."
                        : "Sua solicitação já está na fila de atendimento humano. Um especialista irá atendê-lo em instantes.");

            if (novaSolicitacao) {
                mensagemRepository.save(new Mensagem(conversa.getId(), "ASSISTENTE", textoAviso));
            }

            humanHandoffService.notificarEvento(new TransbordoEventDTO(
                    novaSolicitacao ? "SOLICITACAO_HUMANO" : "NOVA_MENSAGEM_CLIENTE",
                    conversa.getId(),
                    empresa.getId(),
                    conversa.getClienteNome(),
                    conversa.getStatus(),
                    msgSanitizada,
                    "USUARIO"
            ));

            slackWebhookService.notificarTransbordoAssincrono(empresa, conversa, msgSanitizada);

            try {
                emitter.send(SseEmitter.event().name("token").data(com.assistente.empresarial.dto.StreamChunkDTO.token(textoAviso)));
                emitter.send(SseEmitter.event().name("done").data(com.assistente.empresarial.dto.StreamChunkDTO.done(
                        java.util.Map.of("conversaId", conversa.getId(), "latenciaMs", 0, "modelo", "HUMANO")
                )));
                emitter.complete();
            } catch (Exception ignored) {
                emitter.complete();
            }
            return emitter;
        }

        if (empresa.getCreditosTokens() <= 0) {
            throw new BusinessException("Os créditos de tokens da empresa estão esgotados.");
        }

        long streamStart = System.currentTimeMillis();
        RagRetrievalResult ragResult = advancedRagService.retrieveRelevantContext(empresa.getId(), assistente.getId(), msgSanitizada);
        long ragElapsed = System.currentTimeMillis() - streamStart;
        String promptSistema = montarPromptSistema(empresa, assistente, ragResult.getContextText());
        List<ChatMessage> messages = contextWindowService.buildWindowedMessages(conversa.getId(), promptSistema, msgSanitizada);

        int estimatedTokens = Math.max(1, contextWindowService.estimateTokens(promptSistema) + contextWindowService.estimateTokens(msgSanitizada));
        empresa.debitarTokensChat(estimatedTokens);
        empresaRepository.save(empresa);

        ChatTrace streamTrace = new ChatTrace();
        streamTrace.setEmpresaId(empresa.getId());
        streamTrace.setAssistenteId(assistente.getId());
        streamTrace.setCanal("WIDGET_STREAM");
        streamTrace.setRagRetrievalTimeMs(ragElapsed);
        streamTrace.setTotalLatencyMs(ragElapsed);
        streamTrace.setInputTokens(estimatedTokens);
        streamTrace.setOutputTokens(0);
        streamTrace.setTotalTokens(estimatedTokens);
        observabilityService.recordTrace(streamTrace);

        if (streamingChatService != null) {
            streamingChatService.streamResponse(messages, conversa, ragResult.getSources(), emitter);
        }
        return emitter;
    }

    // =========================================================================
    // Operações do Painel de Transbordo Humano em Tempo Real (SSE)
    // =========================================================================

    public SseEmitter conectarPainelTransbordoEmpresa() {
        UUID empresaId = obterEmpresaIdContexto();
        return humanHandoffService.conectarPainelEmpresa(empresaId);
    }

    public SseEmitter conectarEventosConversaPublica(UUID conversaId) {
        return humanHandoffService.conectarConversaCliente(conversaId);
    }

    public List<Conversa> listarFilaTransbordoHumano() {
        UUID empresaId = obterEmpresaIdContexto();
        return conversaRepository.findByEmpresaIdAndStatusInOrderByUpdatedAtDesc(
                empresaId,
                List.of(StatusConversa.AGUARDANDO_HUMANO, StatusConversa.EM_ATENDIMENTO_HUMANO)
        );
    }

    @Transactional
    public Conversa assumirAtendimentoHumano(UUID conversaId) {
        UUID empresaId = obterEmpresaIdContexto();
        Conversa conversa = conversaRepository.findByIdAndEmpresaId(conversaId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada ou não pertence à sua empresa."));

        conversa.setStatus(StatusConversa.EM_ATENDIMENTO_HUMANO);
        conversa.setUpdatedAt(LocalDateTime.now());
        Conversa salva = conversaRepository.save(conversa);

        String aviso = "Um especialista humano assumiu o seu atendimento.";
        mensagemRepository.save(new Mensagem(conversa.getId(), "ATENDENTE", aviso));

        humanHandoffService.notificarEvento(new TransbordoEventDTO(
                "ATENDIMENTO_ASSUMIDO",
                conversa.getId(),
                empresaId,
                conversa.getClienteNome(),
                StatusConversa.EM_ATENDIMENTO_HUMANO,
                aviso,
                "ATENDENTE"
        ));

        return salva;
    }

    @Transactional
    public Mensagem responderComoAtendenteHumano(UUID conversaId, String textoMensagem) {
        if (textoMensagem == null || textoMensagem.trim().isEmpty()) {
            throw new BusinessException("A mensagem do atendente não pode ser vazia.");
        }
        UUID empresaId = obterEmpresaIdContexto();
        Conversa conversa = conversaRepository.findByIdAndEmpresaId(conversaId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada ou não pertence à sua empresa."));

        conversa.setStatus(StatusConversa.EM_ATENDIMENTO_HUMANO);
        conversa.setUpdatedAt(LocalDateTime.now());
        conversaRepository.save(conversa);

        Mensagem msgAtendente = new Mensagem(conversa.getId(), "ATENDENTE", textoMensagem.trim());
        Mensagem salva = mensagemRepository.save(msgAtendente);

        humanHandoffService.notificarEvento(new TransbordoEventDTO(
                "RESPOSTA_ATENDENTE",
                conversa.getId(),
                empresaId,
                conversa.getClienteNome(),
                StatusConversa.EM_ATENDIMENTO_HUMANO,
                salva.getConteudo(),
                "ATENDENTE"
        ));

        return salva;
    }

    @Transactional
    public Conversa encerrarAtendimentoHumano(UUID conversaId, boolean devolverParaBot) {
        UUID empresaId = obterEmpresaIdContexto();
        Conversa conversa = conversaRepository.findByIdAndEmpresaId(conversaId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada ou não pertence à sua empresa."));

        StatusConversa novoStatus = devolverParaBot ? StatusConversa.BOT : StatusConversa.FINALIZADA;
        conversa.setStatus(novoStatus);
        conversa.setUpdatedAt(LocalDateTime.now());
        Conversa salva = conversaRepository.save(conversa);

        String aviso = devolverParaBot
                ? "Atendimento humano concluído. O assistente virtual voltou a ficar disponível para novas dúvidas."
                : "Atendimento encerrado. Agradecemos o seu contato!";
        mensagemRepository.save(new Mensagem(conversa.getId(), "ASSISTENTE", aviso));

        humanHandoffService.notificarEvento(new TransbordoEventDTO(
                "ATENDIMENTO_ENCERRADO",
                conversa.getId(),
                empresaId,
                conversa.getClienteNome(),
                novoStatus,
                aviso,
                "SISTEMA"
        ));

        return salva;
    }

    private String extrairChaveRateLimit(String slugEmpresa, ChatRequestDTO request) {
        String idCliente = request.getClienteIdentificador();
        if (idCliente == null || idCliente.trim().isEmpty()) {
            idCliente = request.getConversaId() != null ? request.getConversaId().toString() : "anon";
        }
        return slugEmpresa + ":" + idCliente.trim();
    }

    private String montarPromptSistema(Empresa empresa, Assistente assistente, String contexto) {
        StringBuilder sb = new StringBuilder();
        sb.append(assistente.getPromptSistema() != null && !assistente.getPromptSistema().trim().isEmpty()
                ? assistente.getPromptSistema()
                : "Você é um assistente virtual corporativo inteligente da empresa " + empresa.getNome() + ".");

        sb.append("\n\nVocê deve adotar um tom de voz ").append(assistente.getTomVoz()).append(" em todas as respostas.");
        sb.append(" IMPORTANTE: Nunca inicie a frase declarando o seu tom ou repetindo '").append(assistente.getTomVoz()).append("'.");

        sb.append("\n\nDiretrizes de Atendimento, Catálogo e Segurança:");
        sb.append("\n1. Baseie-se estritamente na documentação e no catálogo de produtos fornecidos abaixo. Se não souber ou a informação não estiver expressa, informe cordialmente e ofereça atendimento humano.");
        sb.append("\n2. Não invente ou presuma regras, planos, estoques ou valores inexistentes.");
        sb.append("\n3. Sempre que pertinente, você pode utilizar as ferramentas corporativas integradas (ex: cálculo de orçamentos, status de chamados, horário de atendimento).");

        if (contexto != null && !contexto.trim().isEmpty()) {
            sb.append("\n\n--- DOCUMENTAÇÃO / CATÁLOGO DA EMPRESA ---\n");
            sb.append(contexto);
            sb.append("\n------------------------------------------");
        } else {
            sb.append("\n\nNenhum documento ou item de catálogo específico foi encontrado para esta consulta. Responda cordialmente orientando o cliente ou oferecendo suporte humano.");
        }

        return sb.toString();
    }

    private Conversa obterOuCriarConversa(ChatRequestDTO request, UUID empresaId, UUID assistenteId, String canal) {
        String nomeLimpo = (request.getClienteNome() != null && !request.getClienteNome().trim().isEmpty())
                ? request.getClienteNome().trim()
                : null;
        String emailLimpo = (request.getClienteEmail() != null && !request.getClienteEmail().trim().isEmpty())
                ? request.getClienteEmail().trim()
                : null;

        if (request.getConversaId() != null) {
            Optional<Conversa> conversaOpt = conversaRepository.findByIdAndEmpresaId(request.getConversaId(), empresaId);
            if (conversaOpt.isPresent()) {
                Conversa existente = conversaOpt.get();
                boolean alterado = false;
                if (nomeLimpo != null && (existente.getClienteNome() == null
                        || existente.getClienteNome().isBlank()
                        || "Visitante".equalsIgnoreCase(existente.getClienteNome().trim()))) {
                    existente.setClienteNome(nomeLimpo);
                    alterado = true;
                }
                if (emailLimpo != null && (existente.getClienteEmail() == null || existente.getClienteEmail().isBlank())) {
                    existente.setClienteEmail(emailLimpo);
                    alterado = true;
                }
                return alterado ? conversaRepository.save(existente) : existente;
            }
        }

        Conversa nova = new Conversa();
        nova.setEmpresaId(empresaId);
        nova.setAssistenteId(assistenteId);
        nova.setClienteIdentificador(
                request.getClienteIdentificador() != null && !request.getClienteIdentificador().trim().isEmpty()
                        ? request.getClienteIdentificador().trim()
                        : (emailLimpo != null ? emailLimpo : (nomeLimpo != null ? nomeLimpo : UUID.randomUUID().toString()))
        );
        nova.setClienteNome(nomeLimpo != null ? nomeLimpo : "Visitante");
        nova.setClienteEmail(emailLimpo);
        nova.setCanal(canal);
        nova.setStatus(StatusConversa.BOT);

        return conversaRepository.save(nova);
    }

    private Assistente buscarAssistentePadrao(UUID empresaId) {
        List<Assistente> assistentes = assistenteRepository.findByEmpresaId(empresaId);
        if (!assistentes.isEmpty()) {
            return assistentes.get(0);
        }

        Assistente padrao = new Assistente();
        padrao.setEmpresaId(empresaId);
        padrao.setNome("Assistente Virtual");
        padrao.setDescricao("Assistente de atendimento padrão");
        padrao.setPromptSistema("Você é um assistente virtual atencioso e pronto para ajudar.");
        padrao.setCorPrimaria("#2563EB");
        padrao.setCorSecundaria("#1E40AF");
        padrao.setMensagemBoasVindas("Olá! Como posso ajudar você hoje?");
        padrao.setTomVoz("AMIGAVEL");
        padrao.setAtivo(true);

        return assistenteRepository.save(padrao);
    }

    public List<Conversa> listarConversasEmpresa() {
        UUID empresaId = obterEmpresaIdContexto();
        return conversaRepository.findByEmpresaIdOrderByUpdatedAtDesc(empresaId);
    }

    public List<Mensagem> listarMensagensConversa(UUID conversaId) {
        UUID empresaId = obterEmpresaIdContexto();
        conversaRepository.findByIdAndEmpresaId(conversaId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada ou não pertence à sua empresa."));
        return mensagemRepository.findByConversaIdOrderByCreatedAtAsc(conversaId);
    }

    private UUID obterEmpresaIdContexto() {
        UUID empresaId = TenantContext.getTenantId();
        if (empresaId == null) {
            throw new BusinessException("Contexto de empresa não identificado.");
        }
        return empresaId;
    }

    private boolean checarSolicitacaoHumano(String mensagem) {
        String msgLower = mensagem.toLowerCase();
        return msgLower.contains("falar com atendente")
                || msgLower.contains("falar com humano")
                || msgLower.contains("atendimento humano")
                || msgLower.contains("suporte humano")
                || msgLower.contains("falar com uma pessoa")
                || msgLower.contains("chamar atendente")
                || msgLower.contains("humano");
    }

    public StructuredOutputService getStructuredOutputService() {
        return structuredOutputService;
    }

    public RateLimitService getRateLimitService() {
        return rateLimitService;
    }
}
