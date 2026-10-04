package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.UsuarioResponseDTO;
import com.assistente.empresarial.model.Usuario;
import com.assistente.empresarial.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    // Retorna a entidade Usuario para que o Controller possa extrair o ID da empresa e gerar o token
    public Usuario validarCredenciais(String email, String senhaLimpa) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            // Verifica a senha via BCrypt e se o usuário está ativo
            if (passwordEncoder.matches(senhaLimpa, usuario.getSenha()) && usuario.isStatus()) {
                return usuario;
            }
        }
        throw new BadCredentialsException("E-mail ou senha inválidos, ou usuário inativo.");
    }

    // Isola a responsabilidade do ModelMapper
    public UsuarioResponseDTO converterParaDTO(Usuario usuario) {
        return modelMapper.map(usuario, UsuarioResponseDTO.class);
    }
}