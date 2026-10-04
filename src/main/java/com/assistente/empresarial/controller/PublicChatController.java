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
    @CrossOrigin(origins = "*") // Permite que widgets externos em qualquer site consumam a API
    public class PublicChatController {

        private final ChatService chatService;

        public PublicChatController(ChatService chatService) {
            this.chatService = chatService;
        }

        @GetMapping("/{slug}/config")
        public ResponseEntity<WidgetConfigResponseDTO> obterConfiguracaoWidget(@PathVariable String slug) {
            WidgetConfigResponseDTO config = chatService.obterConfiguracaoWidget(slug);
            return ResponseEntity.ok(config);
        }

        @PostMapping("/{slug}/mensagem")
        public ResponseEntity<ChatResponseDTO> enviarMensagem(
                @PathVariable String slug,
                @Valid @RequestBody ChatRequestDTO request) {

            ChatResponseDTO response = chatService.responderPublico(slug, request);
            return ResponseEntity.ok(response);
        }
    }
