package com.assistente.empresarial.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.assistente.empresarial.model.Tenant;
import com.assistente.empresarial.repository.TenantRepository;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Optional<Tenant> validarEBuscarPorApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Optional.empty();
        }

        // Aqui você pode adicionar validações futuras, ex: cache, status, bloqueio por inadimplência, etc.
        Optional<Tenant> tenantOpt = tenantRepository.findByApiKey(apiKey);
        
        if (tenantOpt.isPresent() && tenantOpt.get().isActive()) {
            return tenantOpt;
        }

        return Optional.empty();
    }
}
