package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.AnaliseSentimentoDTO;
import com.assistente.empresarial.dto.LeadQualificadoDTO;
import com.assistente.empresarial.dto.TicketSuporteDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço de Saída Estruturada (Structured Output).
 * - Força o modelo de linguagem a responder estritamente em formato JSON tipado e validado.
 * - Deserializa e valida automaticamente para DTOs Java fortemente tipados.
 * - Extração de Leads, Análise de Sentimento e Abertura de Tickets estruturados.
 */
@Service
public class StructuredOutputService {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputService.class);

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    public StructuredOutputService(ChatLanguageModel chatLanguageModel, @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Extrai um objeto estruturado do tipo T a partir de um texto ou histórico de conversa.
     */
    public <T> T extractStructured(String inputContent, Class<T> targetClass, String instructions) {
        String schemaHint = "Responda APENAS com um objeto JSON válido correspondente à classe " + targetClass.getSimpleName()
                + ". Não inclua blocos markdown como ```json ou qualquer outro texto antes ou depois.";

        String systemPrompt = (instructions != null ? instructions + "\n\n" : "") + schemaHint;

        try {
            Response<dev.langchain4j.data.message.AiMessage> response = chatLanguageModel.generate(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(inputContent)
            );

            String rawJson = cleanJsonOutput(response.content().text());
            log.debug("JSON extraído da LLM para classe {}: {}", targetClass.getSimpleName(), rawJson);

            return objectMapper.readValue(rawJson, targetClass);
        } catch (Exception e) {
            log.warn("Falha ao extrair saída estruturada para {}: {}. Tentando fallback de instância.", targetClass.getSimpleName(), e.getMessage());
            try {
                return targetClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("Não foi possível instanciar saída estruturada para " + targetClass.getSimpleName(), e);
            }
        }
    }

    public AnaliseSentimentoDTO analisarSentimento(String conversaOuMensagem) {
        String instructions = "Você é um classificador analítico de experiência do cliente. Avalie o sentimento (POSITIVO, NEUTRO ou NEGATIVO), pontuação de 1 a 10, se o cliente está em risco de cancelamento/reclamação, o motivo e a ação recomendada.";
        return extractStructured(conversaOuMensagem, AnaliseSentimentoDTO.class, instructions);
    }

    public LeadQualificadoDTO qualificarLead(String conversaOuMensagem) {
        String instructions = "Você é um analista de pré-vendas (SDR). Extraia dados de qualificação do lead como nome, email, telefone, cargo, empresa, tamanho estimado da empresa, nível de interesse (ALTO, MEDIO, BAIXO), resumo da necessidade e orçamento estimado.";
        return extractStructured(conversaOuMensagem, LeadQualificadoDTO.class, instructions);
    }

    public TicketSuporteDTO classificarTicket(String conversaOuMensagem) {
        String instructions = "Você é um triador de suporte técnico. Identifique a categoria (TECNICO, FINANCEIRO, COMERCIAL, DUVIDA), nível de urgência (ALTA, MEDIA, BAIXA), título do chamado, resumo da falha e ações sugeridas de resolução.";
        return extractStructured(conversaOuMensagem, TicketSuporteDTO.class, instructions);
    }

    private String cleanJsonOutput(String output) {
        if (output == null) return "{}";
        String trimmed = output.trim();
        // Remove code fences markdown caso o modelo tenha incluído
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
