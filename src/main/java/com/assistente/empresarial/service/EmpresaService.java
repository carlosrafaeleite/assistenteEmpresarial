package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.EmpresaRegistroRequestDTO;
import com.assistente.empresarial.dto.EmpresaResponseDTO;
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
            throw new RuntimeException("E-mail já registado na plataforma.");
        }
        if (empresaRepository.findBySlug(dto.getSlug()).isPresent()) {
            throw new RuntimeException("Este slug já está em uso.");
        }

        // 2. Criar a Empresa
        Empresa empresa = new Empresa();
        empresa.setNome(dto.getNomeEmpresa());
        empresa.setRazaoSocial(dto.getRazaoSocial());
        empresa.setSlug(dto.getSlug());
        empresa.setEmail(dto.getEmailAdmin()); // E-mail de contacto principal
        empresa.setStatus(true);
        
        Empresa empresaGuardada = empresaRepository.save(empresa);

        // 3. Criar o Utilizador ADMIN vinculado a esta empresa
        Usuario admin = new Usuario();
        admin.setEmpresaId(empresaGuardada.getId());
        admin.setNome(dto.getNomeAdmin());
        admin.setEmail(dto.getEmailAdmin());
        admin.setSenha(passwordEncoder.encode(dto.getSenhaAdmin()));
        admin.setPerfil("ADMIN");
        admin.setStatus(true);
        
        usuarioRepository.save(admin);

        return modelMapper.map(empresaGuardada, EmpresaResponseDTO.class);
    }

    // Método para o cliente atualizar os seus próprios dados após fazer o login
    public EmpresaResponseDTO atualizarMeuPerfil(EmpresaRegistroRequestDTO dto) {
        Empresa empresa = empresaRepository.findById(TenantContext.getTenantId())
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada."));

        empresa.setNome(dto.getNomeEmpresa());
        empresa.setRazaoSocial(dto.getRazaoSocial());
        empresa.setUpdatedAt(LocalDateTime.now());

        Empresa atualizada = empresaRepository.save(empresa);
        return modelMapper.map(atualizada, EmpresaResponseDTO.class);
    }

    public EmpresaResponseDTO obterMeuPerfil() {
        Empresa empresa = empresaRepository.findById(TenantContext.getTenantId())
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada."));
        return modelMapper.map(empresa, EmpresaResponseDTO.class);
    }
}