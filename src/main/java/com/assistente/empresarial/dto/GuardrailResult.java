package com.assistente.empresarial.dto;

import java.util.ArrayList;
import java.util.List;

public class GuardrailResult {

    private final boolean allowed;
    private final String sanitizedText;
    private final List<String> violations;
    private final boolean piiRedacted;

    public GuardrailResult(boolean allowed, String sanitizedText, List<String> violations, boolean piiRedacted) {
        this.allowed = allowed;
        this.sanitizedText = sanitizedText;
        this.violations = violations != null ? violations : new ArrayList<>();
        this.piiRedacted = piiRedacted;
    }

    public static GuardrailResult valid(String text) {
        return new GuardrailResult(true, text, new ArrayList<>(), false);
    }

    public static GuardrailResult validWithSanitization(String text, boolean piiRedacted, List<String> notes) {
        return new GuardrailResult(true, text, notes, piiRedacted);
    }

    public static GuardrailResult blocked(String reason, String fallbackResponse) {
        List<String> violations = new ArrayList<>();
        violations.add(reason);
        return new GuardrailResult(false, fallbackResponse, violations, false);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getSanitizedText() {
        return sanitizedText;
    }

    public List<String> getViolations() {
        return violations;
    }

    public boolean isPiiRedacted() {
        return piiRedacted;
    }
}
