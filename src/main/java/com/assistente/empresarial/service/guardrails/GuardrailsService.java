package com.assistente.empresarial.service.guardrails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Serviço de Guardrails para proteção de entrada e saída.
 * - Detecção de Prompt Injection e Jailbreak
 * - Anonimização/Ofuscação de PII (CPF, Cartões de Crédito, Senhas)
 * - Validação e saneamento de saída contra vazamento de prompt de sistema
 */
@Service
public class GuardrailsService {

    private static final Logger log = LoggerFactory.getLogger(GuardrailsService.class);

    // Padrões de Prompt Injection e Jailbreak
    private static final Pattern[] PROMPT_INJECTION_PATTERNS = {
            Pattern.compile("(?i)ignore (all )?(previous|prior) instructions"),
            Pattern.compile("(?i)desconsidere (todas )?(as )?(instruções|regras|diretrizes) anteriores"),
            Pattern.compile("(?i)esqueça (tudo|todas as regras|suas instruções)"),
            Pattern.compile("(?i)revele (o |seu )?prompt( de sistema)?"),
            Pattern.compile("(?i)show (me )?(your )?(system )?prompt"),
            Pattern.compile("(?i)(jailbreak|dan mode|developer mode|modo desenvolvedor)"),
            Pattern.compile("(?i)you are now (an unrestricted|a rogue|a bypass)"),
            Pattern.compile("(?i)finja que você não tem restrições"),
            Pattern.compile("(?i)hacker mode|bypass security"),
            Pattern.compile("(?i)system override")
    };

    // Padrões de PII
    private static final Pattern CPF_PATTERN = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b");
    private static final Pattern CREDENTIAL_PATTERN = Pattern.compile("(?i)\\b(senha|password|api[_-]?key|secret|bearer)\\s*[:=]\\s*\\S+");

    /**
     * Valida e sanitiza a mensagem de entrada do usuário.
     */
    public GuardrailResult validateInput(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return GuardrailResult.blocked("Mensagem vazia", "Por favor, digite uma mensagem válida.");
        }

        // 1. Verificação de Prompt Injection
        for (Pattern pattern : PROMPT_INJECTION_PATTERNS) {
            if (pattern.matcher(rawMessage).find()) {
                log.warn("Guardrail bloqueou tentativa de prompt injection: {}", rawMessage);
                return GuardrailResult.blocked(
                        "Tentativa de manipulação de instruções detectada (Prompt Injection)",
                        "Não posso processar essa solicitação, pois viola nossas diretrizes de segurança. Como posso ajudar com os serviços da empresa?"
                );
            }
        }

        // 2. Anonimização / Redação de PII
        String sanitized = rawMessage;
        boolean piiFound = false;
        List<String> notes = new ArrayList<>();

        if (CPF_PATTERN.matcher(sanitized).find()) {
            sanitized = CPF_PATTERN.matcher(sanitized).replaceAll("[CPF PROTEGIDO]");
            piiFound = true;
            notes.add("CPF anonimizado para proteção de dados");
        }

        if (CREDIT_CARD_PATTERN.matcher(sanitized).find()) {
            sanitized = CREDIT_CARD_PATTERN.matcher(sanitized).replaceAll("[CARTÃO PROTEGIDO]");
            piiFound = true;
            notes.add("Número de cartão anonimizado para proteção de dados");
        }

        if (CREDENTIAL_PATTERN.matcher(sanitized).find()) {
            sanitized = CREDENTIAL_PATTERN.matcher(sanitized).replaceAll("$1: [CREDENCIAL OCULTA]");
            piiFound = true;
            notes.add("Credencial sensível ocultada");
        }

        if (piiFound) {
            log.info("Guardrail anonimizou dados sensíveis na mensagem do usuário.");
            return GuardrailResult.validWithSanitization(sanitized, true, notes);
        }

        return GuardrailResult.valid(sanitized);
    }

    /**
     * Valida e higieniza a saída gerada pela LLM antes de entregar ao usuário.
     */
    public String validateAndSanitizeOutput(String aiOutput, String systemPrompt) {
        if (aiOutput == null) {
            return "Desculpe, ocorreu uma instabilidade ao gerar a resposta.";
        }

        String sanitized = aiOutput;

        // Se a IA acidentalmente repetir trechos inteiros da instrução de segurança ou prompt mestre
        if (systemPrompt != null && systemPrompt.length() > 50) {
            String snippet = systemPrompt.substring(0, Math.min(80, systemPrompt.length()));
            if (sanitized.contains(snippet)) {
                log.warn("Guardrail detectou vazamento de prompt do sistema na resposta gerada!");
                sanitized = sanitized.replace(snippet, "");
            }
        }

        // Remove menções acidentais a chaves ou tokens
        if (CREDENTIAL_PATTERN.matcher(sanitized).find()) {
            sanitized = CREDENTIAL_PATTERN.matcher(sanitized).replaceAll("$1: [DADO RESTRITO]");
        }

        return sanitized.trim();
    }
}
