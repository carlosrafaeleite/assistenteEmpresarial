package com.assistente.empresarial.repository;

import com.assistente.empresarial.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findByEmpresaId(UUID empresaId);

    long countByEmpresaId(UUID empresaId);

    @Query("SELECT COALESCE(SUM(d.tamanhoBytes), 0) FROM Documento d WHERE d.empresaId = :empresaId")
    long sumTamanhoBytesByEmpresaId(@Param("empresaId") UUID empresaId);

    List<Documento> findByEmpresaIdAndAssistenteId(UUID empresaId, UUID assistenteId);

    Optional<Documento> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<Documento> findByEmpresaIdAndAssistenteIdAndNomeOriginalIgnoreCase(
            UUID empresaId, UUID assistenteId, String nomeOriginal);
}
