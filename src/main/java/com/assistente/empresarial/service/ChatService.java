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
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ChatService(EmpresaRepository empresaRepository,
                       AssistenteRepository assistenteRepository,
                       ConversaRepository conversaRepository,
                       MensagemRepository mensagemRepository,
                       ChatLanguageModel chatLanguageModel,
                       EmbeddingModel embeddingModel,
                       EmbeddingStore<TextSegment> embeddingStore) {
        this.empresaRepository = empresaRepository;
        this.assistenteRepository = assistenteRepository;
        this.conversaRepository = conversaRepository;
        this.mensagemRepository = mensagemRepository;
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
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

        // CORREÇÃO: Pega diretamente o nome_bot da tabela Empresa se ele existir, senão usa o do assistente
        String nomeBotReal = (empresa.getNomeBot() != null && !empresa.getNomeBot().trim().isEmpty())
                ? empresa.getNomeBot()
                : assistente.getNome();

        dto.setAssistenteNome(nomeBotReal);
        dto.setCorPrimaria(assistente.getCorPrimaria());
        dto.setCorSecundaria(assistente.getCorSecundaria());
        dto.setAvatarUrl(assistente.getAvatarUrl());
        dto.setMensagemBoasVindas(assistente.getMensagemBoasVindas());
        dto.setTomVoz(assistente.getTomVoz());

        return dto;
    }


    @Transactional
    public ChatResponseDTO responderPublico(String slugEmpresa, String slugAssistente, ChatRequestDTO request) {
        Empresa empresa = empresaRepository.findBySlug(slugEmpresa)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada com slug: " + slugEmpresa));

        Assistente assistente = assistenteRepository.findBySlugAndEmpresaId(slugAssistente, empresa.getId())
                .orElseGet(() -> buscarAssistentePadrao(empresa.getId()));

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

    private ChatResponseDTO executarProcessamentoChat(Empresa empresa, Assistente assistente, Conversa conversa, String mensagemUsuario) {
        // 1. Salvar mensagem do usuário
        Mensagem msgUser = new Mensagem(conversa.getId(), "USUARIO", mensagemUsuario);
        mensagemRepository.save(msgUser);

        // 2. Verificar se o usuário solicitou atendimento humano
        boolean solicitouHumano = checarSolicitacaoHumano(mensagemUsuario);
        if (solicitouHumano) {
            conversa.setStatus(StatusConversa.AGUARDANDO_HUMANO);
            conversa.setUpdatedAt(LocalDateTime.now());
            conversaRepository.save(conversa);

            String respostaHumano = "Entendido! Estou transferindo seu atendimento para a nossa equipe de suporte humano. Por favor, aguarde um momento.";
            Mensagem msgBot = new Mensagem(conversa.getId(), "ASSISTENTE", respostaHumano);
            mensagemRepository.save(msgBot);

            return new ChatResponseDTO(conversa.getId(), respostaHumano, Collections.emptyList(), true, StatusConversa.AGUARDANDO_HUMANO);
        }

        // 3. Busca Vetorial (RAG) com isolamento estrito por Empresa e Assistente específico
        List<String> fontes = new ArrayList<>();
        String contextoRecuperado = buscarContextoRelevante(empresa.getId(), assistente.getId(), mensagemUsuario, fontes);

        // 4. Montar o Prompt com Instruções e Contexto
        String promptSistema = montarPromptSistema(empresa, assistente, contextoRecuperado);

        // 5. Chamar LLM (Ollama)
        String respostaIa;
        try {
            Response<AiMessage> aiResponse = chatLanguageModel.generate(
                    SystemMessage.from(promptSistema),
                    UserMessage.from(mensagemUsuario)
            );
            respostaIa = aiResponse.content().text();
        } catch (Exception e) {
            log.error("Erro ao chamar o modelo de chat Ollama: {}", e.getMessage(), e);
            respostaIa = "Desculpe, estou enfrentando uma instabilidade temporária no processamento de respostas. Você gostaria de falar com um atendente humano?";
        }

        // 6. Salvar resposta da IA
        Mensagem msgAssistente = new Mensagem(conversa.getId(), "ASSISTENTE", respostaIa);
        if (!fontes.isEmpty()) {
            msgAssistente.setFontes(String.join(", ", fontes));
        }
        mensagemRepository.save(msgAssistente);

        conversa.setUpdatedAt(LocalDateTime.now());
        conversaRepository.save(conversa);

        return new ChatResponseDTO(conversa.getId(), respostaIa, fontes, false, conversa.getStatus());
    }

    private String buscarContextoRelevante(UUID empresaId, UUID assistenteId, String pergunta, List<String> fontesColetadas) {
        try {
            log.info("Gerando embedding para pergunta e buscando contexto para empresa {} e assistente {}", empresaId, assistenteId);
            Response<Embedding> queryEmbedding = embeddingModel.embed(pergunta);

            // Filtro duplo: Garante que a busca respeita a empresa E o bot específico
            var filtro = MetadataFilterBuilder.metadataKey("empresa_id").isEqualTo(empresaId.toString())
                    .and(MetadataFilterBuilder.metadataKey("assistente_id").isEqualTo(assistenteId.toString()));

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding.content())
                    .maxResults(4)
                    .minScore(0.55)
                    .filter(filtro)
                    .build();

            EmbeddingSearchResult<TextSegment> resultado = embeddingStore.search(searchRequest);

            if (resultado == null || resultado.matches().isEmpty()) {
                log.info("Nenhum trecho de documento encontrado acima do limiar para este assistente.");
                return "";
            }

            StringBuilder sb = new StringBuilder();
            Set<String> fontesUnicas = new LinkedHashSet<>();

            for (EmbeddingMatch<TextSegment> match : resultado.matches()) {
                TextSegment segment = match.embedded();
                sb.append("- ").append(segment.text().trim()).append("\n\n");

                String nomeArquivo = segment.metadata().getString("nome_arquivo");
                if (nomeArquivo != null && !nomeArquivo.trim().isEmpty()) {
                    fontesUnicas.add(nomeArquivo);
                }
            }

            fontesColetadas.addAll(fontesUnicas);
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Falha na busca vetorial no embeddingStore: {}", e.getMessage());
            return "";
        }
    }

    private String montarPromptSistema(Empresa empresa, Assistente assistente, String contexto) {
        StringBuilder sb = new StringBuilder();
        sb.append(assistente.getPromptSistema() != null && !assistente.getPromptSistema().trim().isEmpty()
                ? assistente.getPromptSistema()
                : "Você é um assistente virtual inteligente e atencioso da empresa " + empresa.getNome() + ".");

        // CORREÇÃO: Força o tom de voz, mas proíbe a IA de repetir a palavra
        sb.append("\n\nVocê deve adotar um tom de voz ").append(assistente.getTomVoz()).append(" em todas as respostas.");
        sb.append(" IMPORTANTE: Nunca inicie a frase com a palavra '").append(assistente.getTomVoz()).append("' ou declare o seu tom.");

        sb.append("\n\nDiretrizes estritas de atendimento:");
        sb.append("\n1. Se houver trechos de documentos fornecidos abaixo, responda à dúvida baseando-se estritamente neles.");
        sb.append("\n2. Caso a resposta não esteja nos trechos ou não haja certeza, informe cordialmente que não localizou essa informação e oriente a falar com um humano.");
        sb.append("\n3. Seja claro, conciso e aja de forma muito natural. Não use jargões robóticos.");

        if (contexto != null && !contexto.trim().isEmpty()) {
            sb.append("\n\n--- DOCUMENTAÇÃO / REGRAS DA EMPRESA ---\n");
            sb.append(contexto);
            sb.append("\n---------------------------------------");
        } else {
            sb.append("\n\nNenhum documento específico foi encontrado para esta consulta. Responda cordialmente que não encontrou informações e ofereça suporte com a equipe humana.");
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

        // Criar assistente padrão se a empresa ainda não cadastrou nenhum
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
}