package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.AssistenteRequestDTO;
import com.assistente.empresarial.dto.AssistenteResponseDTO;
import com.assistente.empresarial.service.AssistenteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assistentes")
public class AssistenteController {

    private final AssistenteService assistenteService;

    public AssistenteController(AssistenteService assistenteService) {
        this.assistenteService = assistenteService;
    }

    @PostMapping
    public ResponseEntity<AssistenteResponseDTO> criar(@Valid @RequestBody AssistenteRequestDTO request) {
        AssistenteResponseDTO response = assistenteService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AssistenteResponseDTO>> listar() {
        return ResponseEntity.ok(assistenteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssistenteResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(assistenteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssistenteResponseDTO> atualizar(@PathVariable UUID id, @Valid @RequestBody AssistenteRequestDTO request) {
        return ResponseEntity.ok(assistenteService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        assistenteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}