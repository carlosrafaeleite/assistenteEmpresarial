package com.assistente.empresarial.repository;

	
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.assistente.empresarial.model.Empresa;

	@Repository
	public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {
	    Optional<Empresa> findBySlug(String slug);
	    Optional<Empresa> findByEmail(String email);
	}

