package com.assistente.empresarial.service.eval;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkReportDTO {

    private String suiteName;
    private LocalDateTime executedAt;
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private double overallAccuracyPercent;
    private double avgFaithfulness;
    private double avgAnswerRelevance;
    private List<EvalResultDTO> details = new ArrayList<>();

    public BenchmarkReportDTO() {
        this.executedAt = LocalDateTime.now();
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    public int getPassedTests() {
        return passedTests;
    }

    public void setPassedTests(int passedTests) {
        this.passedTests = passedTests;
    }

    public int getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }

    public double getOverallAccuracyPercent() {
        return overallAccuracyPercent;
    }

    public void setOverallAccuracyPercent(double overallAccuracyPercent) {
        this.overallAccuracyPercent = overallAccuracyPercent;
    }

    public double getAvgFaithfulness() {
        return avgFaithfulness;
    }

    public void setAvgFaithfulness(double avgFaithfulness) {
        this.avgFaithfulness = avgFaithfulness;
    }

    public double getAvgAnswerRelevance() {
        return avgAnswerRelevance;
    }

    public void setAvgAnswerRelevance(double avgAnswerRelevance) {
        this.avgAnswerRelevance = avgAnswerRelevance;
    }

    public List<EvalResultDTO> getDetails() {
        return details;
    }

    public void setDetails(List<EvalResultDTO> details) {
        this.details = details;
    }
}
