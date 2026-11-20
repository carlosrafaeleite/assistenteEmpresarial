package com.assistente.empresarial.repository;

import com.assistente.empresarial.model.Conversa;
import com.assistente.empresarial.enuns.StatusConversa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversaRepository extends JpaRepository<Conversa, UUID> {

    List<Conversa> findByEmpresaIdOrderByUpdatedAtDesc(UUID empresaId);

    List<Conversa> findByEmpresaIdAndStatus(UUID empresaId, StatusConversa status);

    List<Conversa> findByEmpresaIdAndStatusInOrderByUpdatedAtDesc(UUID empresaId, Collection<StatusConversa> statuses);

    Optional<Conversa> findByIdAndEmpresaId(UUID id, UUID empresaId);

    long countByEmpresaId(UUID empresaId);

    List<Conversa> findByCreatedAtBetween(LocalDateTime inicio, LocalDateTime fim);

    List<Conversa> findByEmpresaIdAndCreatedAtBetween(UUID empresaId, LocalDateTime inicio, LocalDateTime fim);
}
