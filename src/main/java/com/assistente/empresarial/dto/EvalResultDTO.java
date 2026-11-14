package com.assistente.empresarial.dto;

public class EvalResultDTO {

    private String pergunta;
    private String resposta;
    private String contexto;

    private double faithfulnessScore;   // Fidelidade ao contexto (0.0 - 1.0)
    private double answerRelevanceScore; // Relevância da resposta (0.0 - 1.0)
    private double contextPrecisionScore;// Precisão do RAG (0.0 - 1.0)
    private double guardrailScore;       // Conformidade com guardrails (0.0 - 1.0)
    private double overallScore;         // Média ponderada (0.0 - 1.0)
    private boolean passed;
    private String feedback;

    public EvalResultDTO() {
    }

    public String getPergunta() {
        return pergunta;
    }

    public void setPergunta(String pergunta) {
        this.pergunta = pergunta;
    }

    public String getResposta() {
        return resposta;
    }

    public void setResposta(String resposta) {
        this.resposta = resposta;
    }

    public String getContexto() {
        return contexto;
    }

    public void setContexto(String contexto) {
        this.contexto = contexto;
    }

    public double getFaithfulnessScore() {
        return faithfulnessScore;
    }

    public void setFaithfulnessScore(double faithfulnessScore) {
        this.faithfulnessScore = faithfulnessScore;
    }

    public double getAnswerRelevanceScore() {
        return answerRelevanceScore;
    }

    public void setAnswerRelevanceScore(double answerRelevanceScore) {
        this.answerRelevanceScore = answerRelevanceScore;
    }

    public double getContextPrecisionScore() {
        return contextPrecisionScore;
    }

    public void setContextPrecisionScore(double contextPrecisionScore) {
        this.contextPrecisionScore = contextPrecisionScore;
    }

    public double getGuardrailScore() {
        return guardrailScore;
    }

    public void setGuardrailScore(double guardrailScore) {
        this.guardrailScore = guardrailScore;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
