    package com.assistente.empresarial.controller;

    import com.assistente.empresarial.dto.EmpresaRegistroRequestDTO;
    import com.assistente.empresarial.dto.EmpresaResponseDTO;
    import com.assistente.empresarial.dto.EmpresaUpdateRequestDTO;
    import com.assistente.empresarial.service.EmpresaService;
    import jakarta.validation.Valid;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    @RestController
    @RequestMapping("/api/empresas")
    public class EmpresaController {

        private final EmpresaService empresaService;

        public EmpresaController(EmpresaService empresaService) {
            this.empresaService = empresaService;
        }

        @PostMapping("/registrar")
        public ResponseEntity<EmpresaResponseDTO> registrar(@Valid @RequestBody EmpresaRegistroRequestDTO dto) {
            EmpresaResponseDTO response = empresaService.registarNovaEmpresa(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/me")
        public ResponseEntity<EmpresaResponseDTO> obterMeuPerfil() {
            EmpresaResponseDTO response = empresaService.obterMeuPerfil();
            return ResponseEntity.ok(response);
        }

        @PutMapping("/me")
        public ResponseEntity<EmpresaResponseDTO> atualizarMeuPerfil(@Valid @RequestBody EmpresaUpdateRequestDTO dto) {
            EmpresaResponseDTO response = empresaService.atualizarMeuPerfil(dto);
            return ResponseEntity.ok(response);
        }
    }
