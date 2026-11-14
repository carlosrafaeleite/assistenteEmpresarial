package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.ObservabilityMetricsDTO;
import com.assistente.empresarial.model.ChatTrace;
import com.assistente.empresarial.service.AiObservabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/observability")
@CrossOrigin(originPatterns = "*")
public class ObservabilityController {

    private final AiObservabilityService observabilityService;

    public ObservabilityController(AiObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<ObservabilityMetricsDTO> getMetrics() {
        return ResponseEntity.ok(observabilityService.getAggregatedMetrics());
    }

    @GetMapping("/traces")
    public ResponseEntity<List<ChatTrace>> getRecentTraces(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(observabilityService.getRecentTraces(limit));
    }
}
