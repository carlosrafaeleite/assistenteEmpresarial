package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.ChatRequestDTO;
import com.assistente.empresarial.dto.ChatResponseDTO;
import com.assistente.empresarial.model.Conversa;
import com.assistente.empresarial.model.Mensagem;
import com.assistente.empresarial.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/mensagem")
    public ResponseEntity<ChatResponseDTO> enviarMensagemInterna(@Valid @RequestBody ChatRequestDTO request) {
        ChatResponseDTO response = chatService.responderInterno(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversas")
    public ResponseEntity<List<Conversa>> listarConversas() {
        return ResponseEntity.ok(chatService.listarConversasEmpresa());
    }

    @GetMapping("/conversas/{id}/mensagens")
    public ResponseEntity<List<Mensagem>> listarMensagens(@PathVariable UUID id) {
        return ResponseEntity.ok(chatService.listarMensagensConversa(id));
    }
}
