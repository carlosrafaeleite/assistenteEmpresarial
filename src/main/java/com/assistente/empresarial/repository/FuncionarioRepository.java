package com.assistente.empresarial.repository;

import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    List<Funcionario> findByEmpresa(Empresa empresa);
}