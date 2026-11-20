package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.AssistenteRequestDTO;
import com.assistente.empresarial.dto.AssistenteResponseDTO;
import com.assistente.empresarial.enuns.ModoIdentificacaoChat;
import com.assistente.empresarial.model.Assistente;
import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.repository.AssistenteRepository;
import com.assistente.empresarial.repository.DocumentoRepository;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.security.TenantContext;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssistenteServiceTest {

    @Mock
    private AssistenteRepository assistenteRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    private PasswordEncoder passwordEncoder;
    private ModelMapper modelMapper;
    private AssistenteService assistenteService;
    private final UUID empresaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        modelMapper = new ModelMapper();
        assistenteService = new AssistenteService(
                assistenteRepository,
                empresaRepository,
                modelMapper,
                documentoRepository,
                fileStorageService,
                embeddingStore,
                passwordEncoder
        );
        TenantContext.setTenantId(empresaId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve criar assistente com senha criptografada e sinalizar protegidoPorSenha=true")
    void deveCriarAssistenteComSenha() {
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        empresa.setLimiteAssistentes(5);
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresa));
        when(assistenteRepository.countByEmpresaId(empresaId)).thenReturn(1L);

        AssistenteRequestDTO request = new AssistenteRequestDTO();
        request.setNome("Agente Confidencial");
        request.setSlug("confidencial");
        request.setSenha("segredo123");

        when(assistenteRepository.save(any(Assistente.class))).thenAnswer(invocation -> {
            Assistente a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AssistenteResponseDTO response = assistenteService.criar(request);

        assertNotNull(response);
        assertTrue(response.isProtegidoPorSenha());
        verify(assistenteRepository).save(argThat(a ->
                a.getSenha() != null && passwordEncoder.matches("segredo123", a.getSenha())
        ));
    }

    @Test
    @DisplayName("Deve criar assistente sem senha e sinalizar protegidoPorSenha=false")
    void deveCriarAssistenteSemSenha() {
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        empresa.setLimiteAssistentes(5);
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresa));
        when(assistenteRepository.countByEmpresaId(empresaId)).thenReturn(0L);

        AssistenteRequestDTO request = new AssistenteRequestDTO();
        request.setNome("Agente Publico");
        request.setSlug("publico");
        request.setSenha(null);

        when(assistenteRepository.save(any(Assistente.class))).thenAnswer(invocation -> {
            Assistente a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AssistenteResponseDTO response = assistenteService.criar(request);

        assertNotNull(response);
        assertFalse(response.isProtegidoPorSenha());
        verify(assistenteRepository).save(argThat(a -> a.getSenha() == null));
    }

    @Test
    @DisplayName("Deve alterar senha do assistente existente")
    void deveAlterarSenhaExistente() {
        UUID assistenteId = UUID.randomUUID();
        Assistente existente = new Assistente();
        existente.setId(assistenteId);
        existente.setEmpresaId(empresaId);
        existente.setNome("Bot Teste");

        when(assistenteRepository.findByIdAndEmpresaId(assistenteId, empresaId)).thenReturn(Optional.of(existente));
        when(assistenteRepository.save(any(Assistente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assistenteService.alterarSenha(assistenteId, "novaSenha456");

        assertTrue(passwordEncoder.matches("novaSenha456", existente.getSenha()));
        verify(assistenteRepository).save(existente);
    }

    @Test
    @DisplayName("Deve remover senha quando novaSenha for nula ou vazia")
    void deveRemoverSenhaExistente() {
        UUID assistenteId = UUID.randomUUID();
        Assistente existente = new Assistente();
        existente.setId(assistenteId);
        existente.setEmpresaId(empresaId);
        existente.setSenha(passwordEncoder.encode("senhaAntiga"));

        when(assistenteRepository.findByIdAndEmpresaId(assistenteId, empresaId)).thenReturn(Optional.of(existente));
        when(assistenteRepository.save(any(Assistente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assistenteService.alterarSenha(assistenteId, "");

        assertNull(existente.getSenha());
        assertFalse(existente.temSenha());
        verify(assistenteRepository).save(existente);
    }

    @Test
    @DisplayName("Deve configurar e alterar o modo de identificação do visitante no assistente")
    void deveConfigurarEAlterarModoIdentificacao() {
        UUID assistenteId = UUID.randomUUID();
        Assistente existente = new Assistente();
        existente.setId(assistenteId);
        existente.setEmpresaId(empresaId);
        existente.setNome("Canal RH");
        existente.setSlug("rh");

        assertEquals(ModoIdentificacaoChat.NOME_CONTATO_OPCIONAL, existente.getModoIdentificacao());

        when(assistenteRepository.findByIdAndEmpresaId(assistenteId, empresaId)).thenReturn(Optional.of(existente));
        when(assistenteRepository.save(any(Assistente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssistenteResponseDTO resp = assistenteService.alterarModoIdentificacao(
                assistenteId,
                ModoIdentificacaoChat.NOME_EMAIL_OBRIGATORIO
        );

        assertEquals(ModoIdentificacaoChat.NOME_EMAIL_OBRIGATORIO, resp.getModoIdentificacao());
        assertEquals(ModoIdentificacaoChat.NOME_EMAIL_OBRIGATORIO, existente.getModoIdentificacao());
    }
}
