package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.*;
import com.assistente.empresarial.enuns.LlmProvider;
import com.assistente.empresarial.enuns.PlanoEmpresa;
import com.assistente.empresarial.enuns.TipoTransacaoCredito;
import com.assistente.empresarial.model.ChatTrace;
import com.assistente.empresarial.model.Conversa;
import com.assistente.empresarial.model.CreditoTransacao;
import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.model.Usuario;
import com.assistente.empresarial.repository.*;
import com.assistente.empresarial.security.SaasOwnerAuthorizationService;
import com.assistente.empresarial.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaasAdminServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AssistenteRepository assistenteRepository;
    @Mock
    private DocumentoRepository documentoRepository;
    @Mock
    private ConversaRepository conversaRepository;
    @Mock
    private ChatTraceJpaRepository chatTraceJpaRepository;
    @Mock
    private CreditoTransacaoRepository creditoTransacaoRepository;

    private SaasOwnerAuthorizationService ownerAuthService;
    private SaasAdminService saasAdminService;

    private Empresa empresaMandesTech;
    private Empresa empresaCliente;
    private Usuario usuarioOwner;

    @BeforeEach
    void setUp() {
        ownerAuthService = new SaasOwnerAuthorizationService(
                usuarioRepository,
                empresaRepository,
                "gerenteaifast@gmail.com",
                "MandesTech"
        );
        saasAdminService = new SaasAdminService(
                ownerAuthService,
                empresaRepository,
                usuarioRepository,
                assistenteRepository,
                documentoRepository,
                conversaRepository,
                chatTraceJpaRepository,
                creditoTransacaoRepository
        );

        empresaMandesTech = new Empresa();
        empresaMandesTech.setId(UUID.randomUUID());
        empresaMandesTech.setNome("MandesTech");
        empresaMandesTech.setEmail("gerenteaifast@gmail.com");
        empresaMandesTech.setSlug("mandestech");
        empresaMandesTech.setStatus(true);
        empresaMandesTech.setPlano(PlanoEmpresa.EMPRESARIAL);
        empresaMandesTech.setCreditosTokens(500_000L);
        empresaMandesTech.setTokensConsumidos(25_000L);
        empresaMandesTech.setLimiteAssistentes(10);

        empresaCliente = new Empresa();
        empresaCliente.setId(UUID.randomUUID());
        empresaCliente.setNome("Cliente Alpha");
        empresaCliente.setEmail("contato@alpha.com");
        empresaCliente.setSlug("alpha");
        empresaCliente.setStatus(true);
        empresaCliente.setPlano(PlanoEmpresa.SIMPLES);
        empresaCliente.setCreditosTokens(8_000L);
        empresaCliente.setTokensConsumidos(92_000L);
        empresaCliente.setLimiteAssistentes(5);

        usuarioOwner = new Usuario();
        usuarioOwner.setId(UUID.randomUUID());
        usuarioOwner.setEmpresaId(empresaMandesTech.getId());
        usuarioOwner.setNome("Gerente AIFast");
        usuarioOwner.setEmail("gerenteaifast@gmail.com");
        usuarioOwner.setPerfil("ADMIN");
        usuarioOwner.setStatus(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void autenticarComo(String email, UUID empresaId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList())
        );
        TenantContext.setTenantId(empresaId);
    }

    @Test
    void deveAutorizarSomenteMandesTechComEmailGerenteAiFast() {
        autenticarComo("gerenteaifast@gmail.com", empresaMandesTech.getId());
        when(usuarioRepository.findByEmailIgnoreCase("gerenteaifast@gmail.com")).thenReturn(Optional.of(usuarioOwner));
        when(empresaRepository.findById(empresaMandesTech.getId())).thenReturn(Optional.of(empresaMandesTech));

        assertTrue(ownerAuthService.isCurrentUserSaasOwner());
        assertEquals("gerenteaifast@gmail.com", ownerAuthService.assertCurrentUserIsSaasOwner().getEmail());
    }

    @Test
    void deveNegarAcessoParaOutroEmailNaMandesTech() {
        autenticarComo("crafaelrosa@gmail.com", empresaMandesTech.getId());

        assertFalse(ownerAuthService.isCurrentUserSaasOwner());
        assertThrows(AccessDeniedException.class, () ->
                saasAdminService.obterVisaoGeral(30, null, null, null)
        );
    }

    @Test
    void deveNegarAcessoParaEmailGerenteEmOutraEmpresa() {
        Usuario usuarioOutraEmpresa = new Usuario();
        usuarioOutraEmpresa.setId(UUID.randomUUID());
        usuarioOutraEmpresa.setEmpresaId(empresaCliente.getId());
        usuarioOutraEmpresa.setEmail("gerenteaifast@gmail.com");
        usuarioOutraEmpresa.setStatus(true);

        autenticarComo("gerenteaifast@gmail.com", empresaCliente.getId());
        when(usuarioRepository.findByEmailIgnoreCase("gerenteaifast@gmail.com")).thenReturn(Optional.of(usuarioOutraEmpresa));
        when(empresaRepository.findById(empresaCliente.getId())).thenReturn(Optional.of(empresaCliente));

        assertFalse(ownerAuthService.isCurrentUserSaasOwner());
        assertThrows(AccessDeniedException.class, () ->
                saasAdminService.obterVisaoGeral(30, null, null, null)
        );
    }

    @Test
    void deveInserirCreditosERegistrarTransacaoAuditada() {
        autenticarComo("gerenteaifast@gmail.com", empresaMandesTech.getId());
        when(usuarioRepository.findByEmailIgnoreCase("gerenteaifast@gmail.com")).thenReturn(Optional.of(usuarioOwner));
        when(empresaRepository.findById(empresaMandesTech.getId())).thenReturn(Optional.of(empresaMandesTech));
        when(empresaRepository.findById(empresaCliente.getId())).thenReturn(Optional.of(empresaCliente));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creditoTransacaoRepository.save(any(CreditoTransacao.class))).thenAnswer(inv -> {
            CreditoTransacao tx = inv.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });
        when(creditoTransacaoRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaCliente.getId()))
                .thenReturn(Collections.emptyList());

        SaasCreditoOperacaoRequestDTO req = new SaasCreditoOperacaoRequestDTO();
        req.setOperacao("ADICIONAR");
        req.setQuantidadeTokens(150_000L);
        req.setMotivo("Recarga contratada via PIX");

        Map<String, Object> result = saasAdminService.operarCreditosCliente(empresaCliente.getId(), req);

        ClienteCotaDTO clienteAtualizado = (ClienteCotaDTO) result.get("cliente");
        CreditoTransacaoDTO txDto = (CreditoTransacaoDTO) result.get("transacao");

        assertEquals(158_000L, clienteAtualizado.getCreditosTokens());
        assertEquals(TipoTransacaoCredito.INSERCAO_ADMIN, txDto.getTipo());
        assertEquals(150_000L, txDto.getQuantidadeTokens());
        assertEquals(8_000L, txDto.getSaldoAnterior());
        assertEquals(158_000L, txDto.getSaldoPosterior());
        assertEquals("gerenteaifast@gmail.com", txDto.getRealizadoPorEmail());

        ArgumentCaptor<CreditoTransacao> captor = ArgumentCaptor.forClass(CreditoTransacao.class);
        verify(creditoTransacaoRepository).save(captor.capture());
        assertEquals("Recarga contratada via PIX", captor.getValue().getMotivo());
    }

    @Test
    void deveRetornarVisaoGeralComMetricasSeriesECotas() {
        autenticarComo("gerenteaifast@gmail.com", empresaMandesTech.getId());
        when(usuarioRepository.findByEmailIgnoreCase("gerenteaifast@gmail.com")).thenReturn(Optional.of(usuarioOwner));
        when(empresaRepository.findById(empresaMandesTech.getId())).thenReturn(Optional.of(empresaMandesTech));
        when(empresaRepository.findAll()).thenReturn(new ArrayList<>(List.of(empresaMandesTech, empresaCliente)));

        ChatTrace traceHoje = new ChatTrace();
        traceHoje.setEmpresaId(empresaCliente.getId());
        traceHoje.setTimestamp(LocalDateTime.now());
        traceHoje.setInputTokens(400);
        traceHoje.setOutputTokens(200);
        traceHoje.setTotalTokens(600);
        traceHoje.setTotalLatencyMs(320L);
        traceHoje.setEstimatedCostUsd(0.00018);
        traceHoje.setProviderUsed(LlmProvider.GEMINI);
        traceHoje.setCanal("WIDGET");

        when(chatTraceJpaRepository.findByTimestampBetweenOrderByTimestampAsc(any(), any()))
                .thenReturn(List.of(traceHoje));

        Conversa conv = new Conversa();
        conv.setId(UUID.randomUUID());
        conv.setEmpresaId(empresaCliente.getId());
        conv.setCreatedAt(LocalDateTime.now());
        when(conversaRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(conv));

        CreditoTransacao txAdmin = new CreditoTransacao(
                empresaCliente.getId(),
                empresaCliente.getNome(),
                TipoTransacaoCredito.INSERCAO_ADMIN,
                100_000L,
                0L,
                100_000L,
                "Créditos iniciais",
                "gerenteaifast@gmail.com"
        );
        when(creditoTransacaoRepository.findAll()).thenReturn(List.of(txAdmin));
        when(creditoTransacaoRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any())).thenReturn(List.of(txAdmin));
        when(creditoTransacaoRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(List.of(txAdmin));

        lenient().when(documentoRepository.countByEmpresaId(any())).thenReturn(0L);
        lenient().when(documentoRepository.sumTamanhoBytesByEmpresaId(any())).thenReturn(0L);
        lenient().when(assistenteRepository.countByEmpresaId(any())).thenReturn(0L);
        lenient().when(usuarioRepository.countByEmpresaId(any())).thenReturn(1L);
        lenient().when(conversaRepository.countByEmpresaId(any())).thenReturn(0L);

        lenient().when(documentoRepository.countByEmpresaId(empresaCliente.getId())).thenReturn(1L);
        lenient().when(documentoRepository.sumTamanhoBytesByEmpresaId(empresaCliente.getId())).thenReturn(5L * 1024 * 1024);
        lenient().when(assistenteRepository.countByEmpresaId(empresaCliente.getId())).thenReturn(2L);
        lenient().when(usuarioRepository.countByEmpresaId(empresaCliente.getId())).thenReturn(3L);
        lenient().when(conversaRepository.countByEmpresaId(empresaCliente.getId())).thenReturn(10L);

        SaasOverviewResponseDTO overview = saasAdminService.obterVisaoGeral(
                7, LocalDate.now().minusDays(6), LocalDate.now(), null
        );

        assertEquals(2L, overview.getTotalEmpresas());
        assertEquals(2L, overview.getEmpresasAtivas());
        assertEquals(1L, overview.getEmpresasPlanoSimples());
        assertEquals(1L, overview.getEmpresasPlanoEmpresarial());
        assertEquals(1L, overview.getEmpresasComCreditoBaixo());
        assertEquals(100_000L, overview.getTotalCreditosInseridosAdmin());
        assertEquals(600L, overview.getTokensConsumidosNoPeriodo());
        assertEquals(1L, overview.getRequisicoesNoPeriodo());
        assertEquals(7, overview.getSerieDiaria().size());
        assertEquals(2, overview.getClientes().size());
        assertEquals(1L, overview.getDistribuicaoProvedores().get("GEMINI"));
    }

    @Test
    void deveAtualizarPlanoLimiteAssistentesEStatusDoCliente() {
        autenticarComo("gerenteaifast@gmail.com", empresaMandesTech.getId());
        when(usuarioRepository.findByEmailIgnoreCase("gerenteaifast@gmail.com")).thenReturn(Optional.of(usuarioOwner));
        when(empresaRepository.findById(empresaMandesTech.getId())).thenReturn(Optional.of(empresaMandesTech));
        when(empresaRepository.findById(empresaCliente.getId())).thenReturn(Optional.of(empresaCliente));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creditoTransacaoRepository.save(any(CreditoTransacao.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creditoTransacaoRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaCliente.getId()))
                .thenReturn(Collections.emptyList());

        SaasAtualizarCotasRequestDTO req = new SaasAtualizarCotasRequestDTO();
        req.setPlano("EMPRESARIAL");
        req.setLimiteAssistentes(15);
        req.setStatus(false);
        req.setConcederFranquiaPlano(true);

        ClienteCotaDTO dto = saasAdminService.atualizarCotasCliente(empresaCliente.getId(), req);

        assertEquals(PlanoEmpresa.EMPRESARIAL, dto.getPlano());
        assertEquals(15, dto.getLimiteAssistentes());
        assertFalse(dto.isStatus());
        assertEquals(500_000L, dto.getCreditosTokens());
        verify(creditoTransacaoRepository).save(any(CreditoTransacao.class));
    }

    @Test
    void deveSalvarEmailDoAdminAoRegistrarNovaEmpresaComModelMapperStrict() {
        org.modelmapper.ModelMapper strictMapper = new org.modelmapper.ModelMapper();
        strictMapper.getConfiguration().setMatchingStrategy(org.modelmapper.convention.MatchingStrategies.STRICT);

        org.springframework.security.crypto.password.PasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        EmpresaService empresaService = new EmpresaService(
                empresaRepository,
                usuarioRepository,
                encoder,
                strictMapper,
                new SlackWebhookService(),
                ownerAuthService,
                creditoTransacaoRepository
        );

        when(usuarioRepository.findByEmailIgnoreCase("gerenteaifast@gmail.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("gerenteaifast@gmail.com")).thenReturn(Optional.empty());
        when(empresaRepository.findByEmail("gerenteaifast@gmail.com")).thenReturn(Optional.empty());
        when(empresaRepository.findBySlug("mandestech-novo")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> {
            Empresa e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creditoTransacaoRepository.save(any(CreditoTransacao.class))).thenAnswer(inv -> inv.getArgument(0));

        EmpresaRegistroRequestDTO dto = new EmpresaRegistroRequestDTO();
        dto.setNomeEmpresa("MandesTech");
        dto.setRazaoSocial("MandesTech Ltda");
        dto.setSlug("mandestech-novo");
        dto.setNomeAdmin("Carlos Rafael");
        dto.setEmailAdmin("gerenteaifast@gmail.com");
        dto.setSenhaAdmin("123456");

        empresaService.registarNovaEmpresa(dto);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertEquals("gerenteaifast@gmail.com", usuarioCaptor.getValue().getEmail());
        assertEquals("Carlos Rafael", usuarioCaptor.getValue().getNome());
        assertEquals("ADMIN", usuarioCaptor.getValue().getPerfil());
    }
}
