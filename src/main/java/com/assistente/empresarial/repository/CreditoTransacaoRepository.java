package com.assistente.empresarial.repository;

import com.assistente.empresarial.model.CreditoTransacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CreditoTransacaoRepository extends JpaRepository<CreditoTransacao, UUID> {

    List<CreditoTransacao> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<CreditoTransacao> findByEmpresaIdOrderByCreatedAtDesc(UUID empresaId, Pageable pageable);

    List<CreditoTransacao> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime inicio, LocalDateTime fim);

    List<CreditoTransacao> findByEmpresaIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID empresaId, LocalDateTime inicio, LocalDateTime fim);

    List<CreditoTransacao> findByEmpresaIdOrderByCreatedAtDesc(UUID empresaId);
}
