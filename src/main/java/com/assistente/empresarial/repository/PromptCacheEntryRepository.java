package com.assistente.empresarial.repository;

import com.assistente.empresarial.model.PromptCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface PromptCacheEntryRepository extends JpaRepository<PromptCacheEntry, String> {

    @Transactional
    void deleteByEmpresaIdAndAssistenteId(UUID empresaId, UUID assistenteId);

    @Transactional
    void deleteByEmpresaId(UUID empresaId);
}
