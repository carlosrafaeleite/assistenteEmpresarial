package com.assistente.empresarial.repository;

import com.assistente.empresarial.model.ChatTrace;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatTraceJpaRepository extends JpaRepository<ChatTrace, String> {

    List<ChatTrace> findAllByOrderByTimestampDesc(Pageable pageable);

    List<ChatTrace> findByTimestampBetweenOrderByTimestampAsc(LocalDateTime inicio, LocalDateTime fim);

    List<ChatTrace> findByEmpresaIdAndTimestampBetweenOrderByTimestampAsc(
            UUID empresaId, LocalDateTime inicio, LocalDateTime fim);
}
