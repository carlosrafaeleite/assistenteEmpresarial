package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.LoginRequestDTO;
import com.assistente.empresarial.dto.LoginResponseDTO;
import com.assistente.empresarial.dto.UsuarioResponseDTO;
import com.assistente.empresarial.model.Usuario;
import com.assistente.empresarial.security.JwtService;
import com.assistente.empresarial.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        
        Usuario usuario = authService.validarCredenciais(request.getEmail(), request.getSenha());
        String token = jwtService.gerarToken(usuario);
        UsuarioResponseDTO usuarioDTO = authService.converterParaDTO(usuario);
        
        return ResponseEntity.ok(new LoginResponseDTO(token, usuarioDTO));
    }
}