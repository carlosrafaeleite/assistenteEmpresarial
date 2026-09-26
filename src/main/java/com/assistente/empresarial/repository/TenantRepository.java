package com.assistente.empresarial.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.assistente.empresarial.model.Tenant;

@Repository
public interface TenantRepository {
	
	Optional<Tenant> findByApiKey(String apiKey);

}
