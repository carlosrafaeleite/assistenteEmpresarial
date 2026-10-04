package com.assistente.empresarial.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.service.EmpresaService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final EmpresaService empresaService;

    // Injetamos o EmpresaService no lugar do antigo TenantService
    public ApiKeyFilter(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {


        String path = request.getRequestURI();


        if (path.startsWith("/api/empresas/registrar") || path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-Key");

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            // Busca a Empresa diretamente pela API Key
            Optional<Empresa> empresaOpt = empresaService.buscarPorApiKey(apiKey);

            if (empresaOpt.isPresent()) {
                Empresa empresa = empresaOpt.get();

                // Setamos o ID da empresa no TenantContext para isolar os dados
                TenantContext.setTenantId(empresa.getId());

                // Autentica a requisição via API Key (Role de Cliente de API)
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            empresa.getSlug(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"status\":401,\"error\":\"Não Autorizado\",\"message\":\"API Key inválida ou empresa inativa.\"}");
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                // É vital limpar a ThreadLocal após a requisição para não vazar dados para outras requisições
                TenantContext.clear();
            }
        }
    }
}