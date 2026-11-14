package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.ChatRequestDTO;
import com.assistente.empresarial.dto.ChatResponseDTO;
import com.assistente.empresarial.dto.ValidarSenhaDTO;
import com.assistente.empresarial.dto.WidgetConfigResponseDTO;
import com.assistente.empresarial.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/publico/chat")
@CrossOrigin(originPatterns = "*")
public class PublicChatController {

    private final ChatService chatService;

    public PublicChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // Rotas com empresaSlug e assistenteSlug
    @GetMapping("/{empresaSlug}/{assistenteSlug}/config")
    public ResponseEntity<WidgetConfigResponseDTO> obterConfiguracaoWidget(
            @PathVariable String empresaSlug,
            @PathVariable String assistenteSlug) {

        WidgetConfigResponseDTO config = chatService.obterConfiguracaoWidget(empresaSlug, assistenteSlug);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/{empresaSlug}/{assistenteSlug}/validar-senha")
    public ResponseEntity<Map<String, Object>> validarSenha(
            @PathVariable String empresaSlug,
            @PathVariable String assistenteSlug,
            @RequestBody ValidarSenhaDTO request) {

        String senha = request != null ? request.getSenha() : null;
        boolean valido = chatService.validarSenhaAssistente(empresaSlug, assistenteSlug, senha);
        if (!valido) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valido", false, "mensagem", "Senha incorreta para este assistente."));
        }
        return ResponseEntity.ok(Map.of("valido", true, "mensagem", "Acesso liberado com sucesso."));
    }

    @PostMapping("/{empresaSlug}/{assistenteSlug}/mensagem")
    public ResponseEntity<ChatResponseDTO> enviarMensagem(
            @PathVariable String empresaSlug,
            @PathVariable String assistenteSlug,
            @Valid @RequestBody ChatRequestDTO request) {

        ChatResponseDTO response = chatService.responderPublico(empresaSlug, assistenteSlug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{empresaSlug}/{assistenteSlug}/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMensagem(
            @PathVariable String empresaSlug,
            @PathVariable String assistenteSlug,
            @Valid @RequestBody ChatRequestDTO request) {

        return chatService.iniciarStreamingPublico(empresaSlug, assistenteSlug, request);
    }

    // Sobrecargas de conveniência para quando apenas empresaSlug é fornecido (assistente padrão)
    @GetMapping("/{empresaSlug}/config")
    public ResponseEntity<WidgetConfigResponseDTO> obterConfiguracaoWidgetPadrao(
            @PathVariable String empresaSlug) {
        return obterConfiguracaoWidget(empresaSlug, "padrao");
    }

    @PostMapping("/{empresaSlug}/validar-senha")
    public ResponseEntity<Map<String, Object>> validarSenhaPadrao(
            @PathVariable String empresaSlug,
            @RequestBody ValidarSenhaDTO request) {
        return validarSenha(empresaSlug, "padrao", request);
    }

    @PostMapping("/{empresaSlug}/mensagem")
    public ResponseEntity<ChatResponseDTO> enviarMensagemPadrao(
            @PathVariable String empresaSlug,
            @Valid @RequestBody ChatRequestDTO request) {
        return enviarMensagem(empresaSlug, "padrao", request);
    }

    @PostMapping(value = "/{empresaSlug}/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMensagemPadrao(
            @PathVariable String empresaSlug,
            @Valid @RequestBody ChatRequestDTO request) {
        return streamMensagem(empresaSlug, "padrao", request);
    }
}
