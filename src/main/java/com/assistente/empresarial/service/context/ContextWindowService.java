package com.assistente.empresarial.service.context;

import com.assistente.empresarial.model.Mensagem;
import com.assistente.empresarial.repository.MensagemRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Gerenciador de Janela de Contexto (Context Window Manager).
 * - Carrega o histórico da conversa a partir do banco de dados.
 * - Aplica janela deslizante (Sliding Window) baseada em contagem de mensagens e orçamento de tokens.
 * - Evita estouro de contexto e compacta mensagens antigas quando necessário.
 */
@Service
public class ContextWindowService {

    private static final Logger log = LoggerFactory.getLogger(ContextWindowService.class);

    // Orçamento padrão de histórico para não estourar a janela do modelo
    private static final int DEFAULT_MAX_HISTORY_MESSAGES = 10;
    private static final int DEFAULT_MAX_HISTORY_TOKENS = 3000;

    private final MensagemRepository mensagemRepository;

    public ContextWindowService(MensagemRepository mensagemRepository) {
        this.mensagemRepository = mensagemRepository;
    }

    /**
     * Monta a lista completa de ChatMessages respeitando a janela de contexto.
     * SystemMessage -> Histórico podado/resumido -> UserMessage atual.
     */
    public List<ChatMessage> buildWindowedMessages(UUID conversaId, String systemPrompt, String currentPrompt, int maxMessages, int maxTokens) {
        List<ChatMessage> fullList = new ArrayList<>();

        // 1. Mensagem de Sistema obrigatória
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            fullList.add(SystemMessage.from(systemPrompt));
        }

        // 2. Histórico da Conversa
        if (conversaId != null) {
            List<ChatMessage> historyMessages = retrieveWindowedHistory(conversaId, maxMessages, maxTokens);
            fullList.addAll(historyMessages);
        }

        // 3. Mensagem atual do Usuário
        fullList.add(UserMessage.from(currentPrompt));

        log.debug("Janela de contexto montada com {} mensagens totais (incluindo sistema e usuário atual)", fullList.size());
        return fullList;
    }

    public List<ChatMessage> buildWindowedMessages(UUID conversaId, String systemPrompt, String currentPrompt) {
        return buildWindowedMessages(conversaId, systemPrompt, currentPrompt, DEFAULT_MAX_HISTORY_MESSAGES, DEFAULT_MAX_HISTORY_TOKENS);
    }

    /**
     * Recupera o histórico do banco e aplica poda por janela deslizante e orçamento de tokens.
     */
    public List<ChatMessage> retrieveWindowedHistory(UUID conversaId, int maxMessages, int maxTokens) {
        List<Mensagem> todasMensagens = mensagemRepository.findByConversaIdOrderByCreatedAtAsc(conversaId);
        if (todasMensagens == null || todasMensagens.isEmpty()) {
            return Collections.emptyList();
        }

        // Se houver mais mensagens do que o limite da janela, pegamos as mais recentes
        int startIndex = Math.max(0, todasMensagens.size() - maxMessages);
        List<Mensagem> janelaMensagens = todasMensagens.subList(startIndex, todasMensagens.size());

        List<ChatMessage> resultado = new ArrayList<>();
        int tokenBudget = maxTokens;

        // Se mensagens foram truncadas antes do startIndex, podemos adicionar uma nota de resumo
        if (startIndex > 0) {
            resultado.add(SystemMessage.from(
                    "[Nota de Contexto]: " + startIndex + " mensagens anteriores desta conversa foram compactadas para otimização da janela de contexto."
            ));
        }

        for (Mensagem msg : janelaMensagens) {
            String conteudo = msg.getConteudo();
            int estimatedTokens = estimateTokens(conteudo);

            if (tokenBudget - estimatedTokens < 0) {
                log.info("Limite de tokens da janela de contexto atingido. Truncando mensagens mais antigas.");
                break;
            }

            tokenBudget -= estimatedTokens;

            if ("USUARIO".equalsIgnoreCase(msg.getRemetente())) {
                resultado.add(UserMessage.from(conteudo));
            } else if ("ASSISTENTE".equalsIgnoreCase(msg.getRemetente())) {
                resultado.add(AiMessage.from(conteudo));
            }
        }

        return resultado;
    }

    /**
     * Estimativa heurística rápida de tokens (aprox. 1 token a cada 4 caracteres para português).
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 4.0);
    }
}
