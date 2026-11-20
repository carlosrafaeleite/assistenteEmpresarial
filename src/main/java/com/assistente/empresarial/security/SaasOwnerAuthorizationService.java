package com.assistente.empresarial.security;

import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.model.Usuario;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Controla o acesso estrito ao Painel Administrativo Master do Dono do SaaS.
 * Apenas a conta da empresa MandesTech com o e-mail gerenteaifast@gmail.com
 * (configuráveis via application.properties) possui autorização.
 */
@Service
public class SaasOwnerAuthorizationService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final String ownerEmail;
    private final String ownerEmpresaNome;

    public SaasOwnerAuthorizationService(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            @Value("${app.saas-owner.email:gerenteaifast@gmail.com}") String ownerEmail,
            @Value("${app.saas-owner.empresa:MandesTech}") String ownerEmpresaNome) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.ownerEmail = ownerEmail != null ? ownerEmail.trim() : "gerenteaifast@gmail.com";
        this.ownerEmpresaNome = ownerEmpresaNome != null ? ownerEmpresaNome.trim() : "MandesTech";
    }

    public boolean isSaasOwner(Usuario usuario, Empresa empresa) {
        if (usuario == null || empresa == null || !usuario.isStatus()) {
            return false;
        }
        if (usuario.getEmpresaId() == null || empresa.getId() == null
                || !usuario.getEmpresaId().equals(empresa.getId())) {
            return false;
        }
        return matchesOwnerEmail(usuario.getEmail()) && matchesOwnerEmpresa(empresa.getNome());
    }

    public boolean isSaasOwner(Usuario usuario) {
        if (usuario == null || !usuario.isStatus() || !matchesOwnerEmail(usuario.getEmail())) {
            return false;
        }
        if (usuario.getEmpresaId() == null) {
            return false;
        }
        return empresaRepository.findById(usuario.getEmpresaId())
                .map(empresa -> matchesOwnerEmpresa(empresa.getNome()))
                .orElse(false);
    }

    public boolean isCurrentUserSaasOwner() {
        return getAuthenticatedOwner().isPresent();
    }

    public Usuario assertCurrentUserIsSaasOwner() {
        return getAuthenticatedOwner()
                .orElseThrow(() -> new AccessDeniedException(
                        "Acesso restrito ao painel administrativo do proprietário do SaaS."));
    }

    public Optional<Usuario> getAuthenticatedOwner() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        String email = authentication.getName();
        if (!matchesOwnerEmail(email)) {
            return Optional.empty();
        }
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return Optional.empty();
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailIgnoreCase(email.trim());
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = usuarioRepository.findByEmail(email.trim());
        }
        if (usuarioOpt.isEmpty()) {
            return Optional.empty();
        }
        Usuario usuario = usuarioOpt.get();
        if (!usuario.isStatus() || !tenantId.equals(usuario.getEmpresaId())) {
            return Optional.empty();
        }

        Optional<Empresa> empresaOpt = empresaRepository.findById(tenantId);
        if (empresaOpt.isEmpty() || !matchesOwnerEmpresa(empresaOpt.get().getNome())) {
            return Optional.empty();
        }

        return Optional.of(usuario);
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getOwnerEmpresaNome() {
        return ownerEmpresaNome;
    }

    private boolean matchesOwnerEmail(String email) {
        return email != null && email.trim().equalsIgnoreCase(ownerEmail);
    }

    private boolean matchesOwnerEmpresa(String nomeEmpresa) {
        return nomeEmpresa != null && nomeEmpresa.trim().equalsIgnoreCase(ownerEmpresaNome);
    }
}
