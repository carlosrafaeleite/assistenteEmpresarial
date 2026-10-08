package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.DocumentoResponseDTO;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.ResourceNotFoundException;
import com.assistente.empresarial.model.Documento;
import com.assistente.empresarial.enuns.StatusDocumento;
import com.assistente.empresarial.repository.DocumentoRepository;
import com.assistente.empresarial.security.TenantContext;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    private final DocumentoRepository documentoRepository;
    private final FileStorageService fileStorageService;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ModelMapper modelMapper;

    public DocumentoService(DocumentoRepository documentoRepository,
                            FileStorageService fileStorageService,
                            EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore,
                            ModelMapper modelMapper) {
        this.documentoRepository = documentoRepository;
        this.fileStorageService = fileStorageService;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.modelMapper = modelMapper;
    }

    public DocumentoResponseDTO fazerUploadEProcessar(MultipartFile file, UUID assistenteId) {
        UUID empresaId = obterEmpresaIdContexto();

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessException("Nome do arquivo inválido.");
        }

        String extensao = extrairExtensao(originalFilename).toLowerCase();
        if (!isExtensaoPermitida(extensao)) {
            throw new BusinessException("Tipo de arquivo não suportado. Formato aceito apenas: .pdf");
        }

        // Limite de 50MB
        long limiteMaximoBytes = 50L * 1024 * 1024;
        if (file.getSize() > limiteMaximoBytes) {
            throw new BusinessException("O ficheiro excede o limite máximo permitido de 50MB.");
        }

        // Verificar se já existe um documento associado a este assistente
        List<Documento> documentosExistentes = documentoRepository.findByEmpresaIdAndAssistenteId(empresaId, assistenteId);

        Documento documento;
        if (!documentosExistentes.isEmpty()) {
            // Como a regra é um documento por assistente (ou manter o padrão), pegamos o existente
            documento = documentosExistentes.get(0);

            // Validação estrita: O nome tem de ser exatamente igual ao anterior
            if (!documento.getNomeOriginal().equalsIgnoreCase(originalFilename)) {
                throw new BusinessException(
                        String.format("Para atualizar este assistente, o ficheiro tem de ter exatamente o nome '%s'.", documento.getNomeOriginal())
                );
            }

            // 1. Remover o ficheiro antigo do disco
            try {
                fileStorageService.excluir(documento.getCaminhoArquivo());
            } catch (Exception e) {
                log.warn("Erro ao excluir ficheiro antigo do disco: {}", e.getMessage());
            }

            // 2. Remover vetores antigos do embeddingStore (pgvector)
            try {
                embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("documento_id").isEqualTo(documento.getId().toString()));
            } catch (Exception e) {
                log.warn("Não foi possível excluir vetores antigos no embeddingStore: {}", e.getMessage());
            }

        } else {
            // Se não existir, criamos um novo
            documento = new Documento();
            documento.setEmpresaId(empresaId);
            documento.setAssistenteId(assistenteId);
        }

        String nomeSalvo = UUID.randomUUID() + "_" + originalFilename;
        String caminhoArquivo = fileStorageService.armazenar(file, empresaId, nomeSalvo);

        // Atualizar metadados do documento (reutilizando o ID se já existir)
        documento.setNomeOriginal(originalFilename);
        documento.setNomeSalvo(nomeSalvo);
        documento.setCaminhoArquivo(caminhoArquivo);
        documento.setTipoConteudo(file.getContentType());
        documento.setTamanhoBytes(file.getSize());
        documento.setStatus(StatusDocumento.PROCESSANDO);
        documento.setMensagemErro(null);

        Documento docSalvo = documentoRepository.save(documento);

        // 3. Extração, fatiamento e embeddings da nova versão
        try {
            processarDocumentoRAG(docSalvo, file.getInputStream());
            docSalvo.setStatus(StatusDocumento.PROCESSADO);
            docSalvo.setUpdatedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Erro ao processar embeddings do documento {}: {}", docSalvo.getId(), e.getMessage(), e);
            docSalvo.setStatus(StatusDocumento.ERRO);
            docSalvo.setMensagemErro(e.getMessage());
            docSalvo.setUpdatedAt(LocalDateTime.now());
        }

        Documento docAtualizado = documentoRepository.save(docSalvo);
        return modelMapper.map(docAtualizado, DocumentoResponseDTO.class);
    }

    private void processarDocumentoRAG(Documento documento, InputStream inputStream) {
        log.info("Iniciando extração de texto com Apache Tika para documento {}", documento.getNomeOriginal());
        ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
        Document tikaDoc = parser.parse(inputStream);

        if (tikaDoc.text() == null || tikaDoc.text().trim().isEmpty()) {
            throw new BusinessException("O documento não contém texto legível.");
        }

        // Fatiamento recursivo (800 caracteres por pedaço com 100 caracteres de sobreposição)
        DocumentSplitter splitter = DocumentSplitters.recursive(800, 100);
        List<TextSegment> rawSegments = splitter.split(tikaDoc);

        log.info("Documento dividido em {} partes para vetorização", rawSegments.size());

        List<TextSegment> segmentsComMetadados = new ArrayList<>();
        for (TextSegment raw : rawSegments) {
            Metadata metadata = new Metadata();
            metadata.put("empresa_id", documento.getEmpresaId().toString());
            if (documento.getAssistenteId() != null) {
                metadata.put("assistente_id", documento.getAssistenteId().toString());
            }
            metadata.put("documento_id", documento.getId().toString());
            metadata.put("nome_arquivo", documento.getNomeOriginal());

            segmentsComMetadados.add(TextSegment.from(raw.text(), metadata));
        }

        // Geração de embeddings com Ollama
        log.info("Gerando embeddings via modelo Ollama...");
        Response<List<Embedding>> embeddingResponse = embeddingModel.embedAll(segmentsComMetadados);

        // Armazenamento no banco vetorial
        log.info("Gravando vetores no banco vetorial...");
        embeddingStore.addAll(embeddingResponse.content(), segmentsComMetadados);

        documento.setTotalChunks(segmentsComMetadados.size());
    }

    public List<DocumentoResponseDTO> listarPorEmpresa() {
        UUID empresaId = obterEmpresaIdContexto();
        return documentoRepository.findByEmpresaId(empresaId)
                .stream()
                .map(doc -> modelMapper.map(doc, DocumentoResponseDTO.class))
                .collect(Collectors.toList());
    }

    public void excluir(UUID id) {
        UUID empresaId = obterEmpresaIdContexto();
        Documento documento = documentoRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado ou não pertence à sua empresa."));

        // Remove do disco
        fileStorageService.excluir(documento.getCaminhoArquivo());

        // Remove vetores do embeddingStore filtrando por documento_id
        try {
            embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("documento_id").isEqualTo(id.toString()));
        } catch (Exception e) {
            log.warn("Não foi possível excluir vetores por filtro no embeddingStore: {}", e.getMessage());
        }

        documentoRepository.delete(documento);
    }

    private UUID obterEmpresaIdContexto() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Contexto de empresa não identificado para esta operação.");
        }
        return tenantId;
    }

    private String extrairExtensao(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return dotIndex > 0 ? filename.substring(dotIndex) : "";
    }

    private boolean isExtensaoPermitida(String extensao) {
        return extensao.equals(".pdf");
    }
}
