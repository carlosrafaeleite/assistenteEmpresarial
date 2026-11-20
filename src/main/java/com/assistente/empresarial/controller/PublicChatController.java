package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.ChatRequestDTO;
import com.assistente.empresarial.dto.ChatResponseDTO;
import com.assistente.empresarial.dto.ValidarSenhaDTO;
import com.assistente.empresarial.dto.WidgetConfigResponseDTO;
import com.assistente.empresarial.service.ChatService;
import com.assistente.empresarial.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/publico/chat")
@CrossOrigin(originPatterns = "*")
public class PublicChatController {

    private final ChatService chatService;
    private final RateLimitService rateLimitService;

    @Autowired
    public PublicChatController(ChatService chatService, @Autowired(required = false) RateLimitService rateLimitService) {
        this.chatService = chatService;
        this.rateLimitService = rateLimitService != null ? rateLimitService : new RateLimitService();
    }

    public PublicChatController(ChatService chatService) {
        this(chatService, new RateLimitService());
    }

    // Rotas com empresaSlug e assistenteSlug
    @GetMapping("/{empresaSlug}/{assistenteSlug}/config")
    public ResponseEntity<WidgetConfigResponseDTO> obterConfiguracaoWidget(
            @PathVariable String empresaSlug,
            @PathVariable String assistenteSlug,
            HttpServletRequest httpRequest) {

        rateLimitService.verificarLimiteEndpointPublico(extrairIpCliente(httpRequest));
        WidgetConfigResponseDTO config = chatService.obterConfiguracaoWidget(empresaSlug, assistenteSlug);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/{empresaSlug}/{assistenteSlug}/validar-senha")
    public ResponseEntity<Map<String, Object>> validarSenha(
            @PathVariable String empresaSlug,
            @PathVariable String assistenteSlug,
            @RequestBody ValidarSenhaDTO request,
            HttpServletRequest httpRequest) {

        rateLimitService.verificarLimiteEndpointPublico(extrairIpCliente(httpRequest));
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
            @Valid @RequestBody ChatRequestDTO request,
            HttpServletRequest httpRequest) {

        if (request.getClienteIdentificador() == null || request.getClienteIdentificador().isBlank()) {
            request.setClienteIdentificador(extrairIpCliente(httpRequest));
        }
        ChatResponseDTO response = chatService.responderPublico(empresaSlug, assistenteSlug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{empresaSlug}/{assistenteSlug}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMensagem(
            @PathVariable String empresaSlug,
            @PathVariable String assistenteSlug,
            @Valid @RequestBody ChatRequestDTO request,
            HttpServletRequest httpRequest) {

        if (request.getClienteIdentificador() == null || request.getClienteIdentificador().isBlank()) {
            request.setClienteIdentificador(extrairIpCliente(httpRequest));
        }
        return chatService.iniciarStreamingPublico(empresaSlug, assistenteSlug, request);
    }

    /**
     * Canal SSE em tempo real para o cliente no Widget receber respostas do atendente humano.
     */
    @GetMapping(value = "/conversa/{conversaId}/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEventosConversa(@PathVariable UUID conversaId) {
        return chatService.conectarEventosConversaPublica(conversaId);
    }

    // Sobrecargas de conveniência para quando apenas empresaSlug é fornecido (assistente padrão)
    @GetMapping("/{empresaSlug}/config")
    public ResponseEntity<WidgetConfigResponseDTO> obterConfiguracaoWidgetPadrao(
            @PathVariable String empresaSlug,
            HttpServletRequest httpRequest) {
        return obterConfiguracaoWidget(empresaSlug, "padrao", httpRequest);
    }

    @PostMapping("/{empresaSlug}/validar-senha")
    public ResponseEntity<Map<String, Object>> validarSenhaPadrao(
            @PathVariable String empresaSlug,
            @RequestBody ValidarSenhaDTO request,
            HttpServletRequest httpRequest) {
        return validarSenha(empresaSlug, "padrao", request, httpRequest);
    }

    @PostMapping("/{empresaSlug}/mensagem")
    public ResponseEntity<ChatResponseDTO> enviarMensagemPadrao(
            @PathVariable String empresaSlug,
            @Valid @RequestBody ChatRequestDTO request,
            HttpServletRequest httpRequest) {
        return enviarMensagem(empresaSlug, "padrao", request, httpRequest);
    }

    @PostMapping(value = "/{empresaSlug}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMensagemPadrao(
            @PathVariable String empresaSlug,
            @Valid @RequestBody ChatRequestDTO request,
            HttpServletRequest httpRequest) {
        return streamMensagem(empresaSlug, "padrao", request, httpRequest);
    }

    private String extrairIpCliente(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
