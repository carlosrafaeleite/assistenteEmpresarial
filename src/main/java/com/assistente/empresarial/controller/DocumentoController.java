package com.assistente.empresarial.controller;

import com.assistente.empresarial.dto.DocumentoResponseDTO;
import com.assistente.empresarial.service.DocumentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @PostMapping(value = "/upload/{assistenteId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponseDTO> uploadDocumento(
            @PathVariable("assistenteId") UUID assistenteId,
            @RequestParam("file") MultipartFile file) {

        DocumentoResponseDTO response = documentoService.fazerUploadEProcessar(file, assistenteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentoResponseDTO>> listarDocumentos() {
        List<DocumentoResponseDTO> documentos = documentoService.listarPorEmpresa();
        return ResponseEntity.ok(documentos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirDocumento(@PathVariable UUID id) {
        documentoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
