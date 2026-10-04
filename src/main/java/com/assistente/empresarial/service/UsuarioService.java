package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.UsuarioRequestDTO;
import com.assistente.empresarial.dto.UsuarioResponseDTO;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.ConflictException;
import com.assistente.empresarial.exception.ResourceNotFoundException;
import com.assistente.empresarial.model.Usuario;
import com.assistente.empresarial.repository.UsuarioRepository;
import com.assistente.empresarial.security.TenantContext;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    public UsuarioResponseDTO criar(UsuarioRequestDTO request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Este e-mail já está em uso.");
        }

        if (request.getSenha() == null || request.getSenha().trim().isEmpty()) {
            throw new BusinessException("A senha é obrigatória para criar um novo usuário.");
        }

        if (request.getSenha().trim().length() < 6) {
            throw new BusinessException("A senha deve ter no mínimo 6 caracteres.");
        }

        UUID empresaId = obterEmpresaIdContexto();

        Usuario usuario = new Usuario();
        // Amarra o novo funcionário à empresa do usuário logado
        usuario.setEmpresaId(empresaId);
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(request.getSenha().trim()));
        usuario.setPerfil(request.getPerfil() != null ? request.getPerfil().toUpperCase() : "FUNCIONARIO");
        usuario.setStatus(request.isStatus());

        Usuario salvo = usuarioRepository.save(usuario);
        return modelMapper.map(salvo, UsuarioResponseDTO.class);
    }

    public List<UsuarioResponseDTO> listarEquipe() {
        UUID empresaId = obterEmpresaIdContexto();
        return usuarioRepository.findByEmpresaId(empresaId)
                .stream()
                .map(usuario -> modelMapper.map(usuario, UsuarioResponseDTO.class))
                .collect(Collectors.toList());
    }

    public UsuarioResponseDTO atualizar(UUID id, UsuarioRequestDTO request) {
        Usuario usuario = buscarEntidadeSegura(id);

        usuario.setNome(request.getNome());
        if (request.getPerfil() != null && !request.getPerfil().trim().isEmpty()) {
            usuario.setPerfil(request.getPerfil().toUpperCase());
        }
        usuario.setStatus(request.isStatus());
        usuario.setUpdatedAt(LocalDateTime.now());

        // Só atualiza a senha se o gestor enviou uma nova
        if (request.getSenha() != null && !request.getSenha().trim().isEmpty()) {
            if (request.getSenha().trim().length() < 6) {
                throw new BusinessException("A nova senha deve ter no mínimo 6 caracteres.");
            }
            usuario.setSenha(passwordEncoder.encode(request.getSenha().trim()));
        }

        Usuario atualizado = usuarioRepository.save(usuario);
        return modelMapper.map(atualizado, UsuarioResponseDTO.class);
    }

    public void excluir(UUID id) {
        Usuario usuario = buscarEntidadeSegura(id);
        usuarioRepository.delete(usuario);
    }

    private Usuario buscarEntidadeSegura(UUID id) {
        UUID empresaId = obterEmpresaIdContexto();
        return usuarioRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado ou não pertence à sua empresa."));
    }

    private UUID obterEmpresaIdContexto() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Contexto de empresa não identificado para esta operação.");
        }
        return tenantId;
    }
}