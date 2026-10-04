package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.AssistenteRequestDTO;
import com.assistente.empresarial.dto.AssistenteResponseDTO;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.ResourceNotFoundException;
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
        UUID empresaId = obterEmpresaIdContexto();

        Assistente assistente = modelMapper.map(request, Assistente.class);
        assistente.setEmpresaId(empresaId);
        
        Assistente salvo = assistenteRepository.save(assistente);
        return modelMapper.map(salvo, AssistenteResponseDTO.class);
    }

    public List<AssistenteResponseDTO> listarTodos() {
        UUID empresaId = obterEmpresaIdContexto();
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
        if (request.getCorPrimaria() != null) assistente.setCorPrimaria(request.getCorPrimaria());
        if (request.getCorSecundaria() != null) assistente.setCorSecundaria(request.getCorSecundaria());
        if (request.getAvatarUrl() != null) assistente.setAvatarUrl(request.getAvatarUrl());
        if (request.getMensagemBoasVindas() != null) assistente.setMensagemBoasVindas(request.getMensagemBoasVindas());
        if (request.getTomVoz() != null) assistente.setTomVoz(request.getTomVoz());
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
        UUID empresaId = obterEmpresaIdContexto();
        return assistenteRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Assistente não encontrado ou não pertence à empresa."));
    }

    private UUID obterEmpresaIdContexto() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Contexto de empresa não identificado para esta operação.");
        }
        return tenantId;
    }
}