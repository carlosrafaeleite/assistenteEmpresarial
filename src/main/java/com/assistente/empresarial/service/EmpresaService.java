package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.EmpresaRegistroRequestDTO;
import com.assistente.empresarial.dto.EmpresaResponseDTO;
import com.assistente.empresarial.dto.EmpresaUpdateRequestDTO;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.ConflictException;
import com.assistente.empresarial.exception.ResourceNotFoundException;
import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.model.Usuario;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.repository.UsuarioRepository;
import com.assistente.empresarial.security.TenantContext;
import org.modelmapper.ModelMapper;
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

    public EmpresaService(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public EmpresaResponseDTO registarNovaEmpresa(EmpresaRegistroRequestDTO dto) {
        // 1. Validar se o e-mail ou slug já existem (regras de negócio)
        if (usuarioRepository.findByEmail(dto.getEmailAdmin()).isPresent()) {
            throw new ConflictException("E-mail do administrador já registado na plataforma.");
        }
        if (empresaRepository.findBySlug(dto.getSlug()).isPresent()) {
            throw new ConflictException("Este slug de empresa já está em uso.");
        }

        // 2. Mapeamento com ModelMapper para a entidade Empresa
        Empresa empresa = modelMapper.map(dto, Empresa.class);
        empresa.setNome(dto.getNomeEmpresa()); // Ajuste caso o campo no DTO tenha nome diferente da entidade
        empresa.setEmail(dto.getEmailAdmin());
        empresa.setStatus(true);

        Empresa empresaGuardada = empresaRepository.save(empresa);

        // 3. Criar o Utilizador ADMIN vinculado a esta empresa utilizando ModelMapper
        Usuario admin = modelMapper.map(dto, Usuario.class);
        admin.setEmpresaId(empresaGuardada.getId());
        admin.setNome(dto.getNomeAdmin());
        admin.setSenha(passwordEncoder.encode(dto.getSenhaAdmin()));
        admin.setPerfil("ADMIN");
        admin.setStatus(true);

        usuarioRepository.save(admin);

        return modelMapper.map(empresaGuardada, EmpresaResponseDTO.class);
    }

    // Método para o cliente atualizar os seus próprios dados após fazer o login
    @Transactional
    public EmpresaResponseDTO atualizarMeuPerfil(EmpresaUpdateRequestDTO dto) {
        UUID empresaId = obterEmpresaIdContexto();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));

        // Mapeia os campos atualizados do DTO para a entidade existente
        modelMapper.map(dto, empresa);
        empresa.setNome(dto.getNomeEmpresa());
        empresa.setUpdatedAt(LocalDateTime.now());

        Empresa atualizada = empresaRepository.save(empresa);
        return modelMapper.map(atualizada, EmpresaResponseDTO.class);
    }

    // Sobrecarga para compatibilidade
    public EmpresaResponseDTO atualizarMeuPerfil(EmpresaRegistroRequestDTO dto) {
        return atualizarMeuPerfil(new EmpresaUpdateRequestDTO(dto.getNomeEmpresa(), dto.getRazaoSocial(), null));
    }

    public EmpresaResponseDTO obterMeuPerfil() {
        UUID empresaId = obterEmpresaIdContexto();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));
        return modelMapper.map(empresa, EmpresaResponseDTO.class);
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