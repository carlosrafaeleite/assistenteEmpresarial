package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.ChatRequestDTO;
import com.assistente.empresarial.dto.ChatResponseDTO;
import com.assistente.empresarial.dto.WidgetConfigResponseDTO;
import com.assistente.empresarial.enuns.StatusConversa;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.ResourceNotFoundException;
import com.assistente.empresarial.model.*;
import com.assistente.empresarial.repository.AssistenteRepository;
import com.assistente.empresarial.repository.ConversaRepository;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.repository.MensagemRepository;
import com.assistente.empresarial.security.TenantContext;
import com.assistente.empresarial.service.agent.AgentExecutionResult;
import com.assistente.empresarial.service.agent.ToolAgentService;
import com.assistente.empresarial.service.cache.CachedPromptResponse;
import com.assistente.empresarial.service.cache.PromptCacheService;
import com.assistente.empresarial.service.context.ContextWindowService;
import com.assistente.empresarial.service.fallback.FallbackResult;
import com.assistente.empresarial.service.fallback.ResilientLlmService;
import com.assistente.empresarial.service.guardrails.GuardrailResult;
import com.assistente.empresarial.service.guardrails.GuardrailsService;
import com.assistente.empresarial.service.observability.AiObservabilityService;
import com.assistente.empresarial.service.observability.ChatTrace;
import com.assistente.empresarial.service.rag.AdvancedRagService;
import com.assistente.empresarial.service.rag.RagRetrievalResult;
import com.assistente.empresarial.service.streaming.StreamingChatService;
import com.assistente.empresarial.service.structured.StructuredOutputService;
import com.assistente.empresarial.service.tool.EnterpriseToolRegistry;
import com.assistente.empresarial.service.tool.ToolExecutionEngine;
import com.assistente.empresarial.service.vector.VectorStoreManager;
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

    // Componentes de Arquitetura Robusta de IA
    private final GuardrailsService guardrailsService;
    private final PromptCacheService promptCacheService;
    private final ContextWindowService contextWindowService;
    private final AdvancedRagService advancedRagService;
    private final VectorStoreManager vectorStoreManager;
    private final ToolAgentService toolAgentService;
    private final EnterpriseToolRegistry toolRegistry;
    private final ResilientLlmService resilientLlmService;
    private final AiObservabilityService observabilityService;
    private final StreamingChatService streamingChatService;
    private final StructuredOutputService structuredOutputService;

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
                       VectorStoreManager vectorStoreManager,
                       ToolAgentService toolAgentService,
                       EnterpriseToolRegistry toolRegistry,
                       ResilientLlmService resilientLlmService,
                       AiObservabilityService observabilityService,
                       StreamingChatService streamingChatService,
                       StructuredOutputService structuredOutputService) {
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
        this.vectorStoreManager = vectorStoreManager;
        this.toolAgentService = toolAgentService;
        this.toolRegistry = toolRegistry;
        this.resilientLlmService = resilientLlmService;
        this.observabilityService = observabilityService;
        this.streamingChatService = streamingChatService;
        this.structuredOutputService = structuredOutputService;
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

        // Inicializa instâncias auxiliares para execução resiliente fora do container Spring
        this.guardrailsService = new GuardrailsService();
        this.promptCacheService = new PromptCacheService();
        this.contextWindowService = new ContextWindowService(mensagemRepository);
        this.vectorStoreManager = new VectorStoreManager(embeddingStore, null);
        this.advancedRagService = new AdvancedRagService(embeddingModel, vectorStoreManager);
        this.toolRegistry = new EnterpriseToolRegistry(Collections.emptyList());
        ToolExecutionEngine toolEngine = new ToolExecutionEngine(toolRegistry);
        this.toolAgentService = new ToolAgentService(chatLanguageModel, toolRegistry, toolEngine);
        this.resilientLlmService = null;
        this.observabilityService = new AiObservabilityService();
        this.streamingChatService = null;
        this.structuredOutputService = null;
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

        Assistente assistente = (request.getAssistenteId() != null)
                ? assistenteRepository.findByIdAndEmpresaId(request.getAssistenteId(), empresa.getId())
                .orElseGet(() -> buscarAssistentePadrao(empresa.getId()))
                : buscarAssistentePadrao(empresa.getId());

        Conversa conversa = obterOuCriarConversa(request, empresa.getId(), assistente.getId(), "INTERNO");
        return executarProcessamentoChat(empresa, assistente, conversa, request.getMensagem());
    }

    /**
     * Pipeline Unificado de Execução de Chat contendo os 13 pilares de robustez.
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

        // 2. Verificação de transbordo para Atendimento Humano
        if (checarSolicitacaoHumano(mensagemSanitizada)) {
            conversa.setStatus(StatusConversa.AGUARDANDO_HUMANO);
            conversa.setUpdatedAt(LocalDateTime.now());
            conversaRepository.save(conversa);

            String respostaHumano = "Entendido! Estou transferindo seu atendimento para a nossa equipe de suporte humano. Por favor, aguarde um momento.";
            Mensagem msgBot = new Mensagem(conversa.getId(), "ASSISTENTE", respostaHumano);
            mensagemRepository.save(msgBot);

            trace.setTotalLatencyMs(System.currentTimeMillis() - startTime);
            observabilityService.recordTrace(trace);

            return new ChatResponseDTO(conversa.getId(), respostaHumano, Collections.emptyList(), true, StatusConversa.AGUARDANDO_HUMANO);
        }

        // 3. Prompt Caching: Consulta em memória (<1ms) antes de acionar RAG ou LLM
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

        // 4. RAG Avançado: Recuperação com limiar adaptativo, desduplicação e rastreamento de fontes
        long tRag = System.currentTimeMillis();
        RagRetrievalResult ragResult = advancedRagService.retrieveRelevantContext(empresa.getId(), assistente.getId(), mensagemSanitizada);
        trace.setRagRetrievalTimeMs(System.currentTimeMillis() - tRag);

        String contextoRecuperado = ragResult.getContextText();
        List<String> fontes = ragResult.getSources();

        // 5. Janela de Contexto: Montagem de instruções de sistema e poda deslizante por orçamento de tokens
        String promptSistema = montarPromptSistema(empresa, assistente, contextoRecuperado);
        List<ChatMessage> windowedMessages = contextWindowService.buildWindowedMessages(conversa.getId(), promptSistema, mensagemSanitizada);

        int inputEstimatedTokens = contextWindowService.estimateTokens(promptSistema) + contextWindowService.estimateTokens(mensagemSanitizada);
        trace.setInputTokens(inputEstimatedTokens);

        // 6. Agente de Ferramentas & Fallback de Resiliência
        long tLlm = System.currentTimeMillis();
        String respostaBruta;
        try {
            // Executa com Agente de Ferramentas (ReAct loop com catálogo corporativo)
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

        // 7. Guardrails de Saída: Sanitização contra vazamento de prompt de sistema ou segredos
        String respostaFinal = guardrailsService.validateAndSanitizeOutput(respostaBruta, promptSistema);
        trace.setOutputTokens(contextWindowService.estimateTokens(respostaFinal));
        trace.setTotalTokens(trace.getInputTokens() + trace.getOutputTokens());

        // 8. Gravação no Prompt Cache para acelerar consultas futuras idênticas
        promptCacheService.put(empresa.getId(), assistente.getId(), assistente.getPromptSistema(), mensagemSanitizada, respostaFinal, fontes);

        // 9. Persistência da resposta do assistente
        Mensagem msgAssistente = new Mensagem(conversa.getId(), "ASSISTENTE", respostaFinal);
        if (!fontes.isEmpty()) {
            msgAssistente.setFontes(String.join(", ", fontes));
        }
        mensagemRepository.save(msgAssistente);

        conversa.setUpdatedAt(LocalDateTime.now());
        conversaRepository.save(conversa);

        // 10. Observabilidade: Registro do trace da requisição
        trace.setTotalLatencyMs(System.currentTimeMillis() - startTime);
        observabilityService.recordTrace(trace);

        return new ChatResponseDTO(conversa.getId(), respostaFinal, fontes, false, conversa.getStatus());
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

        Conversa conversa = obterOuCriarConversa(request, empresa.getId(), assistente.getId(), "WIDGET_STREAM");

        // Guardrails de Entrada
        GuardrailResult guardrailResult = guardrailsService.validateInput(request.getMensagem());
        String msgSanitizada = guardrailResult.getSanitizedText();

        Mensagem msgUser = new Mensagem(conversa.getId(), "USUARIO", msgSanitizada);
        mensagemRepository.save(msgUser);

        // RAG Avançado
        RagRetrievalResult ragResult = advancedRagService.retrieveRelevantContext(empresa.getId(), assistente.getId(), msgSanitizada);
        String promptSistema = montarPromptSistema(empresa, assistente, ragResult.getContextText());
        List<ChatMessage> messages = contextWindowService.buildWindowedMessages(conversa.getId(), promptSistema, msgSanitizada);

        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutos de timeout
        if (streamingChatService != null) {
            streamingChatService.streamResponse(messages, conversa, ragResult.getSources(), emitter);
        }
        return emitter;
    }

    private String montarPromptSistema(Empresa empresa, Assistente assistente, String contexto) {
        StringBuilder sb = new StringBuilder();
        sb.append(assistente.getPromptSistema() != null && !assistente.getPromptSistema().trim().isEmpty()
                ? assistente.getPromptSistema()
                : "Você é um assistente virtual corporativo inteligente da empresa " + empresa.getNome() + ".");

        sb.append("\n\nVocê deve adotar um tom de voz ").append(assistente.getTomVoz()).append(" em todas as respostas.");
        sb.append(" IMPORTANTE: Nunca inicie a frase declarando o seu tom ou repetindo '").append(assistente.getTomVoz()).append("'.");

        sb.append("\n\nDiretrizes de Atendimento e Segurança:");
        sb.append("\n1. Baseie-se estritamente na documentação fornecida abaixo. Se não souber ou a informação não estiver expressa, informe cordialmente e ofereça atendimento humano.");
        sb.append("\n2. Não invente ou presuma regras, planos ou valores inexistentes.");
        sb.append("\n3. Sempre que pertinente, você pode utilizar as ferramentas corporativas integradas (ex: cálculo de orçamentos, status de chamados, horário de atendimento).");

        if (contexto != null && !contexto.trim().isEmpty()) {
            sb.append("\n\n--- DOCUMENTAÇÃO / REGRAS DA EMPRESA ---\n");
            sb.append(contexto);
            sb.append("\n---------------------------------------");
        } else {
            sb.append("\n\nNenhum documento específico foi encontrado para esta consulta. Responda cordialmente orientando o cliente ou oferecendo suporte humano.");
        }

        return sb.toString();
    }

    private Conversa obterOuCriarConversa(ChatRequestDTO request, UUID empresaId, UUID assistenteId, String canal) {
        if (request.getConversaId() != null) {
            Optional<Conversa> conversaOpt = conversaRepository.findByIdAndEmpresaId(request.getConversaId(), empresaId);
            if (conversaOpt.isPresent()) {
                return conversaOpt.get();
            }
        }

        Conversa nova = new Conversa();
        nova.setEmpresaId(empresaId);
        nova.setAssistenteId(assistenteId);
        nova.setClienteIdentificador(request.getClienteIdentificador() != null ? request.getClienteIdentificador() : UUID.randomUUID().toString());
        nova.setClienteNome(request.getClienteNome());
        nova.setClienteEmail(request.getClienteEmail());
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
        UUID empresaId = TenantContext.getTenantId();
        if (empresaId == null) {
            throw new BusinessException("Contexto de empresa não identificado.");
        }
        return conversaRepository.findByEmpresaIdOrderByUpdatedAtDesc(empresaId);
    }

    public List<Mensagem> listarMensagensConversa(UUID conversaId) {
        UUID empresaId = TenantContext.getTenantId();
        if (empresaId == null) {
            throw new BusinessException("Contexto de empresa não identificado.");
        }
        conversaRepository.findByIdAndEmpresaId(conversaId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada ou não pertence à sua empresa."));
        return mensagemRepository.findByConversaIdOrderByCreatedAtAsc(conversaId);
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
}
