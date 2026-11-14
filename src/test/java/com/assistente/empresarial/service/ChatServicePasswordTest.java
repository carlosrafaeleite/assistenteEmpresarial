package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.ChatRequestDTO;
import com.assistente.empresarial.model.Assistente;
import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.repository.AssistenteRepository;
import com.assistente.empresarial.repository.ConversaRepository;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.repository.MensagemRepository;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServicePasswordTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private AssistenteRepository assistenteRepository;

    @Mock
    private ConversaRepository conversaRepository;

    @Mock
    private MensagemRepository mensagemRepository;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    private PasswordEncoder passwordEncoder;
    private ChatService chatService;

    private final UUID empresaId = UUID.randomUUID();
    private final UUID assistenteId = UUID.randomUUID();
    private final String empresaSlug = "empresa-teste";
    private final String assistenteSlug = "assistente-privado";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        chatService = new ChatService(
                empresaRepository,
                assistenteRepository,
                conversaRepository,
                mensagemRepository,
                chatLanguageModel,
                embeddingModel,
                embeddingStore,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Deve validar senha correta do assistente com sucesso")
    void deveValidarSenhaCorreta() {
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        empresa.setSlug(empresaSlug);

        Assistente assistente = new Assistente();
        assistente.setId(assistenteId);
        assistente.setEmpresaId(empresaId);
        assistente.setSlug(assistenteSlug);
        assistente.setSenha(passwordEncoder.encode("senha123"));

        when(empresaRepository.findBySlug(empresaSlug)).thenReturn(Optional.of(empresa));
        when(assistenteRepository.findBySlugAndEmpresaId(assistenteSlug, empresaId)).thenReturn(Optional.of(assistente));

        boolean resultado = chatService.validarSenhaAssistente(empresaSlug, assistenteSlug, "senha123");
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Deve rejeitar senha incorreta do assistente")
    void deveRejeitarSenhaIncorreta() {
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        empresa.setSlug(empresaSlug);

        Assistente assistente = new Assistente();
        assistente.setId(assistenteId);
        assistente.setEmpresaId(empresaId);
        assistente.setSlug(assistenteSlug);
        assistente.setSenha(passwordEncoder.encode("senha123"));

        when(empresaRepository.findBySlug(empresaSlug)).thenReturn(Optional.of(empresa));
        when(assistenteRepository.findBySlugAndEmpresaId(assistenteSlug, empresaId)).thenReturn(Optional.of(assistente));

        boolean resultado = chatService.validarSenhaAssistente(empresaSlug, assistenteSlug, "senhaErrada");
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Deve permitir acesso livre se o assistente não tiver senha configurada")
    void devePermitirAcessoSeSemSenha() {
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        empresa.setSlug(empresaSlug);

        Assistente assistente = new Assistente();
        assistente.setId(assistenteId);
        assistente.setEmpresaId(empresaId);
        assistente.setSlug(assistenteSlug);
        assistente.setSenha(null);

        when(empresaRepository.findBySlug(empresaSlug)).thenReturn(Optional.of(empresa));
        when(assistenteRepository.findBySlugAndEmpresaId(assistenteSlug, empresaId)).thenReturn(Optional.of(assistente));

        boolean resultado = chatService.validarSenhaAssistente(empresaSlug, assistenteSlug, null);
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException ao responder publico se senha não for informada em assistente protegido")
    void deveLancarExcecaoAoResponderSemSenha() {
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        empresa.setSlug(empresaSlug);

        Assistente assistente = new Assistente();
        assistente.setId(assistenteId);
        assistente.setEmpresaId(empresaId);
        assistente.setSlug(assistenteSlug);
        assistente.setSenha(passwordEncoder.encode("senha123"));

        when(empresaRepository.findBySlug(empresaSlug)).thenReturn(Optional.of(empresa));
        when(assistenteRepository.findBySlugAndEmpresaId(assistenteSlug, empresaId)).thenReturn(Optional.of(assistente));

        ChatRequestDTO request = new ChatRequestDTO();
        request.setMensagem("Olá");
        request.setSenhaAcesso(null);

        assertThrows(BadCredentialsException.class, () ->
                chatService.responderPublico(empresaSlug, assistenteSlug, request)
        );
    }
}
