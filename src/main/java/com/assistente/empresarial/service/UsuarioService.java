package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.UsuarioRequestDTO;
import com.assistente.empresarial.dto.UsuarioResponseDTO;
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
            throw new RuntimeException("Este e-mail já está em uso.");
        }

        Usuario usuario = new Usuario();
        // A mágica da segurança: amarra o novo funcionário à empresa do admin logado
        usuario.setEmpresaId(TenantContext.getTenantId());
        
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(request.getSenha())); // Criptografa a senha
        usuario.setPerfil(request.getPerfil());
        usuario.setStatus(request.isStatus());
        
        Usuario salvo = usuarioRepository.save(usuario);
        return modelMapper.map(salvo, UsuarioResponseDTO.class);
    }

    public List<UsuarioResponseDTO> listarEquipe() {
        UUID empresaId = TenantContext.getTenantId();
        return usuarioRepository.findByEmpresaId(empresaId)
                .stream()
                .map(usuario -> modelMapper.map(usuario, UsuarioResponseDTO.class))
                .collect(Collectors.toList());
    }

    public UsuarioResponseDTO atualizar(UUID id, UsuarioRequestDTO request) {
        Usuario usuario = buscarEntidadeSegura(id);

        usuario.setNome(request.getNome());
        usuario.setPerfil(request.getPerfil());
        usuario.setStatus(request.isStatus());
        usuario.setUpdatedAt(LocalDateTime.now());
        
        // Só atualiza a senha se o gestor enviou uma nova
        if (request.getSenha() != null && !request.getSenha().trim().isEmpty()) {
            usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        }

        Usuario atualizado = usuarioRepository.save(usuario);
        return modelMapper.map(atualizado, UsuarioResponseDTO.class);
    }

    public void excluir(UUID id) {
        Usuario usuario = buscarEntidadeSegura(id);
        usuarioRepository.delete(usuario);
    }

    private Usuario buscarEntidadeSegura(UUID id) {
        UUID empresaId = TenantContext.getTenantId();
        return usuarioRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado ou não pertence à sua empresa."));
    }
}