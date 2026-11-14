package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.AnaliseSentimentoDTO;
import com.assistente.empresarial.dto.ChatRequestDTO;
import com.assistente.empresarial.dto.ChatResponseDTO;
import com.assistente.empresarial.dto.LeadQualificadoDTO;
import com.assistente.empresarial.dto.TicketSuporteDTO;
import com.assistente.empresarial.enuns.LlmProvider;
import com.assistente.empresarial.model.Conversa;
import com.assistente.empresarial.model.Mensagem;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.repository.VectorStoreRepository;
import com.assistente.empresarial.service.ChatService;
import com.assistente.empresarial.service.StructuredOutputService;
import com.assistente.empresarial.service.llm.LlmRouterService;
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
    private final EmpresaRepository empresaRepository;
    private final LlmRouterService llmRouterService;
    private final VectorStoreRepository vectorStoreRepository;
    private final StructuredOutputService structuredOutputService;

    public ChatController(ChatService chatService,
                          EmpresaRepository empresaRepository,
                          LlmRouterService llmRouterService,
                          VectorStoreRepository vectorStoreRepository,
                          StructuredOutputService structuredOutputService) {
        this.chatService = chatService;
        this.empresaRepository = empresaRepository;
        this.llmRouterService = llmRouterService;
        this.vectorStoreRepository = vectorStoreRepository;
        this.structuredOutputService = structuredOutputService;
    }

    @GetMapping("/llm/status")
    public ResponseEntity<Map<String, Object>> obterStatusLlm() {
        return ResponseEntity.ok(llmRouterService.getStatus());
    }

    @PostMapping("/llm/provedor")
    public ResponseEntity<Map<String, Object>> alterarProvedorLlm(@RequestParam String provedor) {
        llmRouterService.setActiveProvider(LlmProvider.fromString(provedor));
        return ResponseEntity.ok(llmRouterService.getStatus());
    }

    @GetMapping("/vetores/status")
    public ResponseEntity<Map<String, Object>> obterStatusBancoVetorial() {
        return ResponseEntity.ok(vectorStoreRepository.getHealthAndStats());
    }

    @PostMapping("/mensagem")
    public ResponseEntity<ChatResponseDTO> enviarMensagemInterna(@Valid @RequestBody ChatRequestDTO request) {
        ChatResponseDTO response = chatService.responderInterno(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/structured/lead")
    public ResponseEntity<LeadQualificadoDTO> extrairLead(@RequestBody Map<String, String> payload) {
        String texto = payload.getOrDefault("texto", "");
        return ResponseEntity.ok(structuredOutputService.qualificarLead(texto));
    }

    @PostMapping("/structured/sentimento")
    public ResponseEntity<AnaliseSentimentoDTO> analisarSentimento(@RequestBody Map<String, String> payload) {
        String texto = payload.getOrDefault("texto", "");
        return ResponseEntity.ok(structuredOutputService.analisarSentimento(texto));
    }

    @PostMapping("/structured/ticket")
    public ResponseEntity<TicketSuporteDTO> classificarTicket(@RequestBody Map<String, String> payload) {
        String texto = payload.getOrDefault("texto", "");
        return ResponseEntity.ok(structuredOutputService.classificarTicket(texto));
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
                    widgetData.put("empresaNome", empresa.getNome());
                    widgetData.put("razaoSocial", empresa.getRazaoSocial());
                    widgetData.put("corPrimaria", "#0d9488");
                    widgetData.put("nomeBot", empresa.getNomeBot());
                    return ResponseEntity.ok(widgetData);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
