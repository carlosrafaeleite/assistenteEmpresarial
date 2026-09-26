package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.AssistenteRequestDTO;
import com.assistente.empresarial.dto.AssistenteResponseDTO;
import com.assistente.empresarial.model.Assistente;
import com.assistente.empresarial.repository.AssistenteRepository;
import com.assistente.empresarial.security.TenantContext;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AssistenteService {

    private final AssistenteRepository assistenteRepository;
    private final ModelMapper modelMapper;

    public AssistenteService(AssistenteRepository assistenteRepository, ModelMapper modelMapper) {
        this.assistenteRepository = assistenteRepository;
        this.modelMapper = modelMapper;
    }

    public AssistenteResponseDTO criar(AssistenteRequestDTO request) {
        Assistente assistente = modelMapper.map(request, Assistente.class);
        
        // Pega o ID da empresa logada direto do JWT extraído no filtro
        assistente.setEmpresaId(TenantContext.getTenantId());
        
        Assistente salvo = assistenteRepository.save(assistente);
        return modelMapper.map(salvo, AssistenteResponseDTO.class);
    }

    public List<AssistenteResponseDTO> listarTodos() {
        UUID empresaId = TenantContext.getTenantId();
        return assistenteRepository.findByEmpresaId(empresaId)
                .stream()
                .map(assistente -> modelMapper.map(assistente, AssistenteResponseDTO.class))
                .collect(Collectors.toList());
    }

    public AssistenteResponseDTO buscarPorId(UUID id) {
        Assistente assistente = buscarEntidadeSegura(id);
        return modelMapper.map(assistente, AssistenteResponseDTO.class);
    }

    public AssistenteResponseDTO atualizar(UUID id, AssistenteRequestDTO request) {
        Assistente assistente = buscarEntidadeSegura(id);
        
        assistente.setNome(request.getNome());
        assistente.setDescricao(request.getDescricao());
        assistente.setPromptSistema(request.getPromptSistema());
        assistente.setAtivo(request.isAtivo());
        assistente.setUpdatedAt(LocalDateTime.now());
        
        Assistente atualizado = assistenteRepository.save(assistente);
        return modelMapper.map(atualizado, AssistenteResponseDTO.class);
    }

    public void excluir(UUID id) {
        Assistente assistente = buscarEntidadeSegura(id);
        assistenteRepository.delete(assistente);
    }

    // Método privado centralizado para garantir o isolamento dos dados
    private Assistente buscarEntidadeSegura(UUID id) {
        UUID empresaId = TenantContext.getTenantId();
        return assistenteRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RuntimeException("Assistente não encontrado ou não pertence à empresa."));
    }
}