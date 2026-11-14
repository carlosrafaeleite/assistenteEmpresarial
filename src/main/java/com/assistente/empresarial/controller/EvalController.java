package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.BenchmarkReportDTO;
import com.assistente.empresarial.dto.EvalResultDTO;
import com.assistente.empresarial.service.LlmEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/evals")
@CrossOrigin(originPatterns = "*")
public class EvalController {

    private final LlmEvaluationService evaluationService;

    public EvalController(LlmEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/run-benchmark")
    public ResponseEntity<BenchmarkReportDTO> runBenchmark() {
        BenchmarkReportDTO report = evaluationService.runBenchmarkSuite();
        return ResponseEntity.ok(report);
    }

    @PostMapping("/evaluate")
    public ResponseEntity<EvalResultDTO> evaluateSingle(@RequestBody Map<String, String> payload) {
        String pergunta = payload.getOrDefault("pergunta", "");
        String contexto = payload.getOrDefault("contexto", "");
        String resposta = payload.getOrDefault("resposta", "");

        EvalResultDTO result = evaluationService.evaluate(pergunta, contexto, resposta);
        return ResponseEntity.ok(result);
    }
}
