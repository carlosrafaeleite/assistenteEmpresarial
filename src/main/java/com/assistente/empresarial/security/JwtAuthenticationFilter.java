package com.assistente.empresarial.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Se não tem token ou não começa com "Bearer ", passa direto (o Spring Security vai barrar depois se a rota for privada)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        userEmail = jwtService.extrairEmail(jwt);

        // Se tem e-mail no token e o usuário ainda não está autenticado no contexto do Spring
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            if (jwtService.isTokenValido(jwt, userEmail)) {
                // Multi-Tenant: Lê a empresa do token e define na thread atual
                String empresaId = jwtService.extrairEmpresaId(jwt);
                if (empresaId != null) {
                    TenantContext.setTenantId(UUID.fromString(empresaId));
                }

                // Carrega a autoridade com base no perfil (ex: ROLE_ADMIN, ROLE_FUNCIONARIO)
                String perfil = jwtService.extrairPerfil(jwt);
                List<GrantedAuthority> authorities = (perfil != null && !perfil.trim().isEmpty())
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + perfil.trim().toUpperCase()))
                        : Collections.emptyList();

                // Avisa ao Spring Security que o usuário está autenticado
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail, null, authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Limpa a thread após a requisição para evitar vazamento de dados de um cliente para outro
            TenantContext.clear();
        }
    }
}