package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.EmpresaRegistroRequestDTO;
import com.assistente.empresarial.dto.EmpresaResponseDTO;
import com.assistente.empresarial.dto.EmpresaUpdateRequestDTO;
import com.assistente.empresarial.enuns.PlanoEmpresa;
import com.assistente.empresarial.enuns.TipoTransacaoCredito;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.ConflictException;
import com.assistente.empresarial.exception.ResourceNotFoundException;
import com.assistente.empresarial.model.CreditoTransacao;
import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.model.Usuario;
import com.assistente.empresarial.repository.CreditoTransacaoRepository;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.repository.UsuarioRepository;
import com.assistente.empresarial.security.SaasOwnerAuthorizationService;
import com.assistente.empresarial.security.TenantContext;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final SlackWebhookService slackWebhookService;
    private final SaasOwnerAuthorizationService saasOwnerAuthorizationService;
    private final CreditoTransacaoRepository creditoTransacaoRepository;

    @Autowired
    public EmpresaService(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder, ModelMapper modelMapper,
                          @Autowired(required = false) SlackWebhookService slackWebhookService,
                          @Autowired(required = false) SaasOwnerAuthorizationService saasOwnerAuthorizationService,
                          @Autowired(required = false) CreditoTransacaoRepository creditoTransacaoRepository) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.slackWebhookService = slackWebhookService != null ? slackWebhookService : new SlackWebhookService();
        this.saasOwnerAuthorizationService = saasOwnerAuthorizationService;
        this.creditoTransacaoRepository = creditoTransacaoRepository;
    }

    public EmpresaService(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder, ModelMapper modelMapper,
                          SlackWebhookService slackWebhookService) {
        this(empresaRepository, usuarioRepository, passwordEncoder, modelMapper, slackWebhookService, null, null);
    }

    public EmpresaService(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this(empresaRepository, usuarioRepository, passwordEncoder, modelMapper, new SlackWebhookService(), null, null);
    }

    @Transactional
    public EmpresaResponseDTO registarNovaEmpresa(EmpresaRegistroRequestDTO dto) {
        String emailAdmin = dto.getEmailAdmin() != null ? dto.getEmailAdmin().trim() : null;
        if (emailAdmin == null || emailAdmin.isEmpty()) {
            throw new BusinessException("O e-mail do administrador é obrigatório.");
        }
        if (usuarioRepository.findByEmailIgnoreCase(emailAdmin).isPresent()
                || usuarioRepository.findByEmail(emailAdmin).isPresent()
                || empresaRepository.findByEmail(emailAdmin).isPresent()) {
            throw new ConflictException("E-mail do administrador já registado na plataforma.");
        }
        if (empresaRepository.findBySlug(dto.getSlug()).isPresent()) {
            throw new ConflictException("Este slug de empresa já está em uso.");
        }

        Empresa empresa = modelMapper.map(dto, Empresa.class);
        empresa.setNome(dto.getNomeEmpresa());
        empresa.setRazaoSocial(dto.getRazaoSocial());
        empresa.setSlug(dto.getSlug());
        empresa.setEmail(emailAdmin);
        empresa.setStatus(true);
        empresa.setPlano(PlanoEmpresa.SIMPLES);
        empresa.setCreditosTokens(PlanoEmpresa.SIMPLES.getCreditosIniciais());
        empresa.setTokensConsumidos(0L);

        Empresa empresaGuardada = empresaRepository.save(empresa);

        Usuario admin = new Usuario();
        admin.setEmpresaId(empresaGuardada.getId());
        admin.setNome(dto.getNomeAdmin());
        admin.setEmail(emailAdmin);
        admin.setSenha(passwordEncoder.encode(dto.getSenhaAdmin()));
        admin.setPerfil("ADMIN");
        admin.setStatus(true);

        usuarioRepository.save(admin);

        registrarTransacaoCredito(
                empresaGuardada,
                TipoTransacaoCredito.FRANQUIA_INICIAL,
                empresaGuardada.getCreditosTokens(),
                0L,
                empresaGuardada.getCreditosTokens(),
                "Franquia inicial do plano " + empresaGuardada.getPlano().name(),
                emailAdmin
        );

        return toResponseDTO(empresaGuardada);
    }

    // IMPORTANTE: Nenhuma configuração ou atualização de perfil desconta créditos/tokens!
    @Transactional
    public EmpresaResponseDTO atualizarMeuPerfil(EmpresaUpdateRequestDTO dto) {
        UUID empresaId = obterEmpresaIdContexto();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));

        empresa.setNome(dto.getNomeEmpresa());
        if (dto.getRazaoSocial() != null) {
            empresa.setRazaoSocial(dto.getRazaoSocial());
        }
        if (dto.getNomeBot() != null) {
            empresa.setNomeBot(dto.getNomeBot());
        }
        if (dto.getSlackWebhookUrl() != null) {
            empresa.setSlackWebhookUrl(dto.getSlackWebhookUrl().trim().isEmpty() ? null : dto.getSlackWebhookUrl().trim());
        }
        if (dto.getPlano() != null && !dto.getPlano().trim().isEmpty()) {
            PlanoEmpresa novoPlano = PlanoEmpresa.fromString(dto.getPlano());
            PlanoEmpresa planoAntigo = empresa.getPlano();
            empresa.setPlano(novoPlano);
            if (novoPlano == PlanoEmpresa.EMPRESARIAL && planoAntigo == PlanoEmpresa.SIMPLES) {
                empresa.setCreditosTokens(Math.max(empresa.getCreditosTokens(), PlanoEmpresa.EMPRESARIAL.getCreditosIniciais()));
            }
        }
        empresa.setUpdatedAt(LocalDateTime.now());

        Empresa atualizada = empresaRepository.save(empresa);
        return toResponseDTO(atualizada);
    }

    public EmpresaResponseDTO atualizarMeuPerfil(EmpresaRegistroRequestDTO dto) {
        return atualizarMeuPerfil(new EmpresaUpdateRequestDTO(dto.getNomeEmpresa(), dto.getRazaoSocial(), null));
    }

    public EmpresaResponseDTO obterMeuPerfil() {
        UUID empresaId = obterEmpresaIdContexto();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));
        return toResponseDTO(empresa);
    }

    @Transactional
    public EmpresaResponseDTO alterarPlano(PlanoEmpresa novoPlano) {
        UUID empresaId = obterEmpresaIdContexto();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));
        long saldoAnterior = empresa.getCreditosTokens();
        empresa.setPlano(novoPlano);
        if (novoPlano == PlanoEmpresa.EMPRESARIAL && empresa.getCreditosTokens() < PlanoEmpresa.EMPRESARIAL.getCreditosIniciais()) {
            empresa.setCreditosTokens(PlanoEmpresa.EMPRESARIAL.getCreditosIniciais());
        }
        empresa.setUpdatedAt(LocalDateTime.now());
        Empresa salva = empresaRepository.save(empresa);
        if (salva.getCreditosTokens() > saldoAnterior) {
            registrarTransacaoCredito(
                    salva,
                    TipoTransacaoCredito.PLANO_UPGRADE,
                    salva.getCreditosTokens() - saldoAnterior,
                    saldoAnterior,
                    salva.getCreditosTokens(),
                    "Ajuste de franquia por upgrade para o plano " + novoPlano.name(),
                    obterEmailUsuarioAtual(salva.getEmail())
            );
        }
        return toResponseDTO(salva);
    }

    @Transactional
    public EmpresaResponseDTO recarregarCreditos(long tokensAdicionais) {
        if (tokensAdicionais <= 0) {
            throw new BusinessException("A quantidade de créditos a adicionar deve ser maior que zero.");
        }
        UUID empresaId = obterEmpresaIdContexto();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));
        long saldoAnterior = empresa.getCreditosTokens();
        empresa.setCreditosTokens(saldoAnterior + tokensAdicionais);
        empresa.setUpdatedAt(LocalDateTime.now());
        Empresa salva = empresaRepository.save(empresa);
        registrarTransacaoCredito(
                salva,
                TipoTransacaoCredito.RECARGA_CLIENTE,
                tokensAdicionais,
                saldoAnterior,
                salva.getCreditosTokens(),
                "Recarga de créditos realizada no painel da empresa",
                obterEmailUsuarioAtual(salva.getEmail())
        );
        return toResponseDTO(salva);
    }

    @Transactional
    public EmpresaResponseDTO atualizarSlackWebhook(String webhookUrl) {
        UUID empresaId = obterEmpresaIdContexto();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));
        String limpo = (webhookUrl != null && !webhookUrl.trim().isEmpty()) ? webhookUrl.trim() : null;
        empresa.setSlackWebhookUrl(limpo);
        empresa.setUpdatedAt(LocalDateTime.now());
        return toResponseDTO(empresaRepository.save(empresa));
    }

    public boolean testarSlackWebhook(String webhookUrlOpcional) {
        UUID empresaId = obterEmpresaIdContexto();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));
        String urlAlvo = (webhookUrlOpcional != null && !webhookUrlOpcional.trim().isEmpty())
                ? webhookUrlOpcional.trim()
                : empresa.getSlackWebhookUrl();
        return slackWebhookService.enviarTesteWebhook(urlAlvo, empresa.getNome());
    }

    private EmpresaResponseDTO toResponseDTO(Empresa empresa) {
        EmpresaResponseDTO dto = modelMapper.map(empresa, EmpresaResponseDTO.class);
        dto.setPlano(empresa.getPlano());
        dto.setCreditosTokens(empresa.getCreditosTokens());
        dto.setTokensConsumidos(empresa.getTokensConsumidos());
        dto.setMaxArquivos(empresa.getMaxArquivosPermitidos());
        dto.setLimiteArmazenamentoBytes(empresa.getLimiteArmazenamentoBytes());
        dto.setSlackWebhookUrl(empresa.getSlackWebhookUrl());
        boolean isOwner = saasOwnerAuthorizationService != null
                && saasOwnerAuthorizationService.isCurrentUserSaasOwner();
        dto.setSaasOwner(isOwner);
        return dto;
    }

    private void registrarTransacaoCredito(Empresa empresa, TipoTransacaoCredito tipo, long delta,
                                           long saldoAnterior, long saldoPosterior, String motivo, String emailAutor) {
        if (creditoTransacaoRepository == null || empresa.getId() == null) {
            return;
        }
        CreditoTransacao transacao = new CreditoTransacao(
                empresa.getId(),
                empresa.getNome() != null ? empresa.getNome() : "Empresa",
                tipo,
                delta,
                saldoAnterior,
                saldoPosterior,
                motivo,
                emailAutor != null ? emailAutor : "sistema"
        );
        creditoTransacaoRepository.save(transacao);
    }

    private String obterEmailUsuarioAtual(String fallback) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
            return auth.getName();
        }
        return fallback != null ? fallback : "sistema";
    }

    private UUID obterEmpresaIdContexto() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Contexto de empresa não identificado para esta operação.");
        }
        return tenantId;
    }

    public Optional<Empresa> buscarPorApiKey(String apiKey) {
        return Optional.empty();
    }
}
