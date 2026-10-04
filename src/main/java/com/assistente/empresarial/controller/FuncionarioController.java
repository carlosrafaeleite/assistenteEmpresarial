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

    @PostMapping
    public ResponseEntity<Funcionario> criar(@Valid @RequestBody FuncionarioRequestDTO dto) {
        Funcionario novo = funcionarioService.criar(dto);
        return ResponseEntity.ok(novo);
    }
}