package com.assistente.empresarial.repository;

import com.assistente.empresarial.model.Assistente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssistenteRepository extends JpaRepository<Assistente, UUID> {
    
    // Lista todos os assistentes de uma empresa específica
    List<Assistente> findByEmpresaId(UUID empresaId);
    
    // Busca um assistente específico GARANTINDO que ele pertence à empresa logada
    Optional<Assistente> findByIdAndEmpresaId(UUID id, UUID empresaId);
}