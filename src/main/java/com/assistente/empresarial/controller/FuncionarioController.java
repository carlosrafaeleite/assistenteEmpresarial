package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.FuncionarioRequestDTO;
import com.assistente.empresarial.model.Funcionario;
import com.assistente.empresarial.service.FuncionarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {

    @Autowired
    private FuncionarioService funcionarioService;

    @GetMapping
    public ResponseEntity<List<Funcionario>> listar() {
        return ResponseEntity.ok(funcionarioService.listarDaEmpresa());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Funcionario> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(funcionarioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Funcionario> criar(@Valid @RequestBody FuncionarioRequestDTO dto) {
        Funcionario novo = funcionarioService.criar(dto);
        return ResponseEntity.ok(novo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Funcionario> atualizar(@PathVariable Long id, @Valid @RequestBody FuncionarioRequestDTO dto) {
        Funcionario atualizado = funcionarioService.atualizar(id, dto);
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        funcionarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}