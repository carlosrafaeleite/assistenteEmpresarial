package com.assistente.empresarial.repository;

import com.assistente.empresarial.model.ChatTrace;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatTraceJpaRepository extends JpaRepository<ChatTrace, String> {

    List<ChatTrace> findAllByOrderByTimestampDesc(Pageable pageable);
}
