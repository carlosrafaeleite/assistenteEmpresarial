package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.ChatRequestDTO;
import com.assistente.empresarial.dto.ChatResponseDTO;
import com.assistente.empresarial.model.Conversa;
import com.assistente.empresarial.model.Mensagem;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final   EmpresaRepository empresaRepository;

    public ChatController(ChatService chatService, EmpresaRepository empresaRepository) {
        this.chatService = chatService;
        this.empresaRepository = empresaRepository;
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

    @GetMapping("/widget/{empresaSlug}")
    public ResponseEntity<?> obterDadosWidget(@PathVariable String empresaSlug) {
        return empresaRepository.findBySlug(empresaSlug)
                .map(empresa -> {
                    Map<String, Object> widgetData = new HashMap<>();
                    widgetData.put("empresaNome", empresa.getNome()); // <--- Aqui está o nome correto da empresa!
                    widgetData.put("razaoSocial", empresa.getRazaoSocial());
                    widgetData.put("corPrimaria", "#0d9488");
                    widgetData.put("nomeBot", empresa.getNomeBot()); // <--- O nome do bot (ex: "Terezinha")
                    return ResponseEntity.ok(widgetData);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
