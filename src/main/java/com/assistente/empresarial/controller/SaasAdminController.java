package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.*;
import com.assistente.empresarial.service.SaasAdminService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/saas-admin")
public class SaasAdminController {

    private final SaasAdminService saasAdminService;

    public SaasAdminController(SaasAdminService saasAdminService) {
        this.saasAdminService = saasAdminService;
    }

    @GetMapping("/overview")
    public ResponseEntity<SaasOverviewResponseDTO> obterVisaoGeral(
            @RequestParam(value = "dias", required = false) Integer dias,
            @RequestParam(value = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(value = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(value = "empresaId", required = false) UUID empresaId) {
        return ResponseEntity.ok(saasAdminService.obterVisaoGeral(dias, dataInicio, dataFim, empresaId));
    }

    @PostMapping("/clientes/{empresaId}/creditos")
    public ResponseEntity<Map<String, Object>> operarCreditos(
            @PathVariable("empresaId") UUID empresaId,
            @RequestBody SaasCreditoOperacaoRequestDTO request) {
        return ResponseEntity.ok(saasAdminService.operarCreditosCliente(empresaId, request));
    }

    @PatchMapping("/clientes/{empresaId}/cotas")
    public ResponseEntity<ClienteCotaDTO> atualizarCotas(
            @PathVariable("empresaId") UUID empresaId,
            @RequestBody SaasAtualizarCotasRequestDTO request) {
        return ResponseEntity.ok(saasAdminService.atualizarCotasCliente(empresaId, request));
    }

    @GetMapping("/creditos/transacoes")
    public ResponseEntity<List<CreditoTransacaoDTO>> listarTransacoes(
            @RequestParam(value = "empresaId", required = false) UUID empresaId,
            @RequestParam(value = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(value = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit) {
        return ResponseEntity.ok(saasAdminService.listarTransacoesCreditos(empresaId, dataInicio, dataFim, limit));
    }
}
