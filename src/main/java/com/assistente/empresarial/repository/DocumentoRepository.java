package com.assistente.empresarial.repository;

import com.assistente.empresarial.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findByEmpresaId(UUID empresaId);

    List<Documento> findByEmpresaIdAndAssistenteId(UUID empresaId, UUID assistenteId);

    Optional<Documento> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
