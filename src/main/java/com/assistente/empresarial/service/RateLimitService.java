package com.assistente.empresarial.service;

import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Serviço de Rate Limiting (Janela Deslizante) e Controle de Tamanho de Perguntas.
 * - Limita o número de caracteres por pergunta (evita abuso de tokens e payloads gigantes).
 * - Limita a quantidade de perguntas por minuto por cliente/IP nos endpoints públicos do Widget.
 * - Protege endpoints públicos de configuração e validação de senha contra ataques de força bruta.
 */
@Service
public class RateLimitService {

    private static final long WINDOW_MS = 60_000L; // 1 minuto

    @Value("${app.chat.max-question-chars:500}")
    private int maxQuestionChars = 500;

    @Value("${app.chat.max-questions-per-minute:10}")
    private int maxQuestionsPerMinute = 10;

    @Value("${app.widget.max-requests-per-minute:60}")
    private int maxWidgetRequestsPerMinute = 60;

    private final Map<String, Deque<Long>> questionWindows = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> publicEndpointWindows = new ConcurrentHashMap<>();

    public RateLimitService() {
    }

    public RateLimitService(int maxQuestionChars, int maxQuestionsPerMinute) {
        this.maxQuestionChars = maxQuestionChars;
        this.maxQuestionsPerMinute = maxQuestionsPerMinute;
    }

    /**
     * Valida o limite de caracteres da pergunta e a taxa de perguntas por minuto do usuário.
     */
    public void validarPerguntaERateLimit(String clientKey, String pergunta) {
        validarTamanhoPergunta(pergunta);
        verificarTaxaPorMinuto(
                questionWindows,
                normalizarChave(clientKey),
                maxQuestionsPerMinute,
                String.format("Limite de %d perguntas por minuto atingido. Aguarde alguns instantes antes de enviar nova mensagem.", maxQuestionsPerMinute)
        );
    }

    /**
     * Valida apenas o limite de caracteres por pergunta.
     */
    public void validarTamanhoPergunta(String pergunta) {
        if (pergunta == null || pergunta.trim().isEmpty()) {
            throw new BusinessException("A pergunta não pode ser vazia.");
        }
        int length = pergunta.trim().length();
        if (length > maxQuestionChars) {
            throw new BusinessException(String.format(
                    "A pergunta excede o limite máximo de %d caracteres (enviado: %d caracteres).",
                    maxQuestionChars, length));
        }
    }

    /**
     * Rate limiting geral para endpoints públicos do Widget (configuração, validação de senha, etc.).
     */
    public void verificarLimiteEndpointPublico(String clientIpOrKey) {
        verificarTaxaPorMinuto(
                publicEndpointWindows,
                normalizarChave(clientIpOrKey),
                maxWidgetRequestsPerMinute,
                "Muitas requisições em sequência a partir deste endereço. Tente novamente em 1 minuto."
        );
    }

    private void verificarTaxaPorMinuto(Map<String, Deque<Long>> store, String key, int maxAllowed, String errorMsg) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = store.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) > WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxAllowed) {
                throw new TooManyRequestsException(errorMsg);
            }
            timestamps.addLast(now);
        }
    }

    private String normalizarChave(String key) {
        return (key != null && !key.trim().isEmpty()) ? key.trim() : "anon-global";
    }

    public int getMaxQuestionChars() {
        return maxQuestionChars;
    }

    public void setMaxQuestionChars(int maxQuestionChars) {
        this.maxQuestionChars = maxQuestionChars;
    }

    public int getMaxQuestionsPerMinute() {
        return maxQuestionsPerMinute;
    }

    public void setMaxQuestionsPerMinute(int maxQuestionsPerMinute) {
        this.maxQuestionsPerMinute = maxQuestionsPerMinute;
    }

    public void limparTudo() {
        questionWindows.clear();
        publicEndpointWindows.clear();
    }
}
