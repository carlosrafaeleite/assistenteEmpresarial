package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.DocumentoResponseDTO;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.ResourceNotFoundException;
import com.assistente.empresarial.model.Documento;
import com.assistente.empresarial.model.StatusDocumento;
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
            throw new BusinessException("Tipo de arquivo não suportado. Formatos aceitos: .pdf, .docx, .txt, .csv, .md");
        }

        String nomeSalvo = UUID.randomUUID() + "_" + originalFilename;
        String caminhoArquivo = fileStorageService.armazenar(file, empresaId, nomeSalvo);

        // 1. Salvar metadados no banco relacional
        Documento documento = new Documento();
        documento.setEmpresaId(empresaId);
        documento.setAssistenteId(assistenteId);
        documento.setNomeOriginal(originalFilename);
        documento.setNomeSalvo(nomeSalvo);
        documento.setCaminhoArquivo(caminhoArquivo);
        documento.setTipoConteudo(file.getContentType());
        documento.setTamanhoBytes(file.getSize());
        documento.setStatus(StatusDocumento.PROCESSANDO);

        Documento docSalvo = documentoRepository.save(documento);

        // 2. Extração, fatiamento e embeddings
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
        return extensao.equals(".pdf") || extensao.equals(".docx") || extensao.equals(".doc")
                || extensao.equals(".txt") || extensao.equals(".csv") || extensao.equals(".md");
    }
}
