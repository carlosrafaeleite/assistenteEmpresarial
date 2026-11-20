package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.EmpresaRegistroRequestDTO;
import com.assistente.empresarial.dto.EmpresaResponseDTO;
import com.assistente.empresarial.dto.EmpresaUpdateRequestDTO;
import com.assistente.empresarial.enuns.PlanoEmpresa;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.ConflictException;
import com.assistente.empresarial.exception.ResourceNotFoundException;
import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.model.Usuario;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.repository.UsuarioRepository;
import com.assistente.empresarial.security.TenantContext;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public EmpresaService(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder, ModelMapper modelMapper,
                          @Autowired(required = false) SlackWebhookService slackWebhookService) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.slackWebhookService = slackWebhookService != null ? slackWebhookService : new SlackWebhookService();
    }

    public EmpresaService(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this(empresaRepository, usuarioRepository, passwordEncoder, modelMapper, new SlackWebhookService());
    }

    @Transactional
    public EmpresaResponseDTO registarNovaEmpresa(EmpresaRegistroRequestDTO dto) {
        if (usuarioRepository.findByEmail(dto.getEmailAdmin()).isPresent()) {
            throw new ConflictException("E-mail do administrador já registado na plataforma.");
        }
        if (empresaRepository.findBySlug(dto.getSlug()).isPresent()) {
            throw new ConflictException("Este slug de empresa já está em uso.");
        }

        Empresa empresa = modelMapper.map(dto, Empresa.class);
        empresa.setNome(dto.getNomeEmpresa());
        empresa.setEmail(dto.getEmailAdmin());
        empresa.setStatus(true);
        empresa.setPlano(PlanoEmpresa.SIMPLES);
        empresa.setCreditosTokens(PlanoEmpresa.SIMPLES.getCreditosIniciais());
        empresa.setTokensConsumidos(0L);

        Empresa empresaGuardada = empresaRepository.save(empresa);

        Usuario admin = modelMapper.map(dto, Usuario.class);
        admin.setEmpresaId(empresaGuardada.getId());
        admin.setNome(dto.getNomeAdmin());
        admin.setSenha(passwordEncoder.encode(dto.getSenhaAdmin()));
        admin.setPerfil("ADMIN");
        admin.setStatus(true);

        usuarioRepository.save(admin);

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
        empresa.setPlano(novoPlano);
        if (novoPlano == PlanoEmpresa.EMPRESARIAL && empresa.getCreditosTokens() < PlanoEmpresa.EMPRESARIAL.getCreditosIniciais()) {
            empresa.setCreditosTokens(PlanoEmpresa.EMPRESARIAL.getCreditosIniciais());
        }
        empresa.setUpdatedAt(LocalDateTime.now());
        return toResponseDTO(empresaRepository.save(empresa));
    }

    @Transactional
    public EmpresaResponseDTO recarregarCreditos(long tokensAdicionais) {
        if (tokensAdicionais <= 0) {
            throw new BusinessException("A quantidade de créditos a adicionar deve ser maior que zero.");
        }
        UUID empresaId = obterEmpresaIdContexto();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));
        empresa.setCreditosTokens(empresa.getCreditosTokens() + tokensAdicionais);
        empresa.setUpdatedAt(LocalDateTime.now());
        return toResponseDTO(empresaRepository.save(empresa));
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
        return dto;
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
