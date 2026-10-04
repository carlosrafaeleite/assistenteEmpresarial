package com.assistente.empresarial.repository;

import com.assistente.empresarial.model.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, UUID> {

    List<Mensagem> findByConversaIdOrderByCreatedAtAsc(UUID conversaId);
}
