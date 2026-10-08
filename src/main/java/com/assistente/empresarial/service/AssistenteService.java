package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.AssistenteRequestDTO;
import com.assistente.empresarial.dto.AssistenteResponseDTO;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.ResourceNotFoundException;
import com.assistente.empresarial.model.Assistente;
import com.assistente.empresarial.model.Documento;
import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.repository.AssistenteRepository;
import com.assistente.empresarial.repository.DocumentoRepository;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.security.TenantContext;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jdk.internal.org.jline.utils.Log;
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
    private final EmpresaRepository empresaRepository;

    private final DocumentoRepository documentoRepository;
    private final FileStorageService fileStorageService;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private Log log;

    public AssistenteService(AssistenteRepository assistenteRepository,
                             EmpresaRepository empresaRepository,
                             ModelMapper modelMapper, DocumentoRepository documentoRepository, FileStorageService fileStorageService, EmbeddingStore<TextSegment> embeddingStore) {
        this.assistenteRepository = assistenteRepository;
        this.empresaRepository = empresaRepository;
        this.modelMapper = modelMapper;
        this.documentoRepository = documentoRepository;
        this.fileStorageService = fileStorageService;
        this.embeddingStore = embeddingStore;
    }

    public AssistenteResponseDTO criar(AssistenteRequestDTO request) {
        UUID empresaId = obterEmpresaIdContexto();

        // 1. Buscar a empresa para obter o limite flexível configurado
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));

        int limiteFlexivel = empresa.getLimiteAssistentes();
        long totalAssistentes = assistenteRepository.countByEmpresaId(empresaId);

        // 2. Validar se ultrapassou o limite contratado
        if (totalAssistentes >= limiteFlexivel) {
            throw new BusinessException(
                    String.format("Atingiu o limite do seu plano (%d assistentes). Faça upgrade ou adquira pacotes adicionais para continuar.", limiteFlexivel)
            );
        }

        // 3. Criar o assistente
        Assistente assistente = modelMapper.map(request, Assistente.class);
        assistente.setEmpresaId(empresaId);

        // FORÇA O MAPEAMENTO DO SLUG PARA EVITAR O ERRO DE NULL
        if (request.getSlug() != null && !request.getSlug().isEmpty()) {
            assistente.setSlug(request.getSlug());
        }

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

        // Garante que o slug é atualizado
        if (request.getSlug() != null && !request.getSlug().isEmpty()) {
            assistente.setSlug(request.getSlug());
        }

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
        UUID empresaId = obterEmpresaIdContexto();

        // 1. Buscar e apagar todos os documentos vinculados a este assistente específico
        List<Documento> documentosDoAssistente = documentoRepository.findByEmpresaIdAndAssistenteId(empresaId, id);
        for (Documento doc : documentosDoAssistente) {
            try {
                fileStorageService.excluir(doc.getCaminhoArquivo());
            } catch (Exception e) {
                log.warn("Erro ao apagar ficheiro do disco: {}", e.getMessage());
            }
            try {
                embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("documento_id").isEqualTo(doc.getId().toString()));
            } catch (Exception e) {
                log.warn("Erro ao apagar vetores: {}", e.getMessage());
            }
            documentoRepository.delete(doc);
        }

        // 2. Apagar o assistente
        assistenteRepository.delete(assistente);
    }

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