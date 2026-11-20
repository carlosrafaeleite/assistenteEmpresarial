package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.EmpresaRegistroRequestDTO;
import com.assistente.empresarial.dto.EmpresaResponseDTO;
import com.assistente.empresarial.dto.EmpresaUpdateRequestDTO;
import com.assistente.empresarial.enuns.PlanoEmpresa;
import com.assistente.empresarial.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @PostMapping("/registrar")
    public ResponseEntity<EmpresaResponseDTO> registrar(@Valid @RequestBody EmpresaRegistroRequestDTO dto) {
        EmpresaResponseDTO response = empresaService.registarNovaEmpresa(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<EmpresaResponseDTO> obterMeuPerfil() {
        EmpresaResponseDTO response = empresaService.obterMeuPerfil();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<EmpresaResponseDTO> atualizarMeuPerfil(@Valid @RequestBody EmpresaUpdateRequestDTO dto) {
        EmpresaResponseDTO response = empresaService.atualizarMeuPerfil(dto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/plano")
    public ResponseEntity<EmpresaResponseDTO> alterarPlano(@RequestParam("plano") String plano) {
        EmpresaResponseDTO response = empresaService.alterarPlano(PlanoEmpresa.fromString(plano));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/creditos")
    public ResponseEntity<EmpresaResponseDTO> adicionarCreditos(@RequestBody Map<String, Long> body) {
        long tokens = body != null && body.get("tokens") != null ? body.get("tokens") : 0L;
        return ResponseEntity.ok(empresaService.recarregarCreditos(tokens));
    }

    @PatchMapping("/me/slack")
    public ResponseEntity<EmpresaResponseDTO> atualizarSlackWebhook(@RequestBody Map<String, String> body) {
        String url = body != null ? body.get("slackWebhookUrl") : null;
        return ResponseEntity.ok(empresaService.atualizarSlackWebhook(url));
    }

    @PostMapping("/me/slack/testar")
    public ResponseEntity<Map<String, Object>> testarSlackWebhook(@RequestBody(required = false) Map<String, String> body) {
        String url = body != null ? body.get("slackWebhookUrl") : null;
        boolean enviado = empresaService.testarSlackWebhook(url);
        return ResponseEntity.ok(Map.of(
                "sucesso", enviado,
                "mensagem", enviado
                        ? "Notificação de teste enviada com sucesso para o Slack!"
                        : "Não foi possível entregar a mensagem no Slack. Verifique a URL do Incoming Webhook."
        ));
    }
}
