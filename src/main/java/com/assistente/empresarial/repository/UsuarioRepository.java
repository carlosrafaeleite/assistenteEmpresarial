package com.assistente.empresarial.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.assistente.empresarial.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	Optional<Usuario> findByEmail(String email);
    
    // Lista toda a equipe da empresa
    List<Usuario> findByEmpresaId(UUID empresaId);
    
    // Busca um usuário específico garantindo que é da empresa logada
    Optional<Usuario> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
