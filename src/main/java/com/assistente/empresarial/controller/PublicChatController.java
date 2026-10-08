package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.ChatRequestDTO;
import com.assistente.empresarial.dto.ChatResponseDTO;
import com.assistente.empresarial.dto.WidgetConfigResponseDTO;
import com.assistente.empresarial.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/publico/chat")
@CrossOrigin(originPatterns = "*")
public class PublicChatController {

    private final ChatService chatService;

    public PublicChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // Rota com empresaSlug e assistenteSlug
    @GetMapping("/{empresaSlug}/{assistenteSlug}/config")
    public ResponseEntity<WidgetConfigResponseDTO> obterConfiguracaoWidget(
            @PathVariable String empresaSlug,
            @PathVariable String assistenteSlug) {

        WidgetConfigResponseDTO config = chatService.obterConfiguracaoWidget(empresaSlug, assistenteSlug);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/{empresaSlug}/{assistenteSlug}/mensagem")
    public ResponseEntity<ChatResponseDTO> enviarMensagem(
            @PathVariable String empresaSlug,
            @PathVariable String assistenteSlug,
            @Valid @RequestBody ChatRequestDTO request) {

        ChatResponseDTO response = chatService.responderPublico(empresaSlug, assistenteSlug, request);
        return ResponseEntity.ok(response);
    }
}