package com.assistente.empresarial.service;

import com.assistente.empresarial.config.RabbitMqConfig;
import com.assistente.empresarial.dto.DocumentoResponseDTO;
import com.assistente.empresarial.dto.DocumentoUploadEventDTO;
import com.assistente.empresarial.enuns.PlanoEmpresa;
import com.assistente.empresarial.enuns.StatusDocumento;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.ResourceNotFoundException;
import com.assistente.empresarial.model.Documento;
import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.repository.DocumentoRepository;
import com.assistente.empresarial.repository.EmpresaRepository;
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
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Serviço de Ingestão de Documentos e Catálogos (Upload 100% Gratuito para o Lojista).
 * - Suporta PDFs (manuais, regras), planilhas Excel/CSV (catálogos de produtos, preços e estoque) e documentos de texto.
 * - Valida limites por plano:
 *   * SIMPLES: até 1 arquivo e limite de 30 MB por empresa.
 *   * EMPRESARIAL: até 5 arquivos e limite de 50 MB por empresa.
 * - Processamento assíncrono via fila RabbitMQ (com fallback resiliente local caso o broker RabbitMQ não esteja ativo).
 * - Geração de embeddings 100% local via Ollama (nomic-embed-text, 768 dimensões) com custo zero de API e sem descontar créditos/tokens do lojista.
 */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    private final DocumentoRepository documentoRepository;
    private final EmpresaRepository empresaRepository;
    private final FileStorageService fileStorageService;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ModelMapper modelMapper;
    private final PromptCacheService promptCacheService;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.enabled:false}")
    private boolean rabbitMqEnabled;

    @Value("${app.upload.async:false}")
    private boolean asyncFallbackEnabled;

    @Autowired
    public DocumentoService(DocumentoRepository documentoRepository,
                            EmpresaRepository empresaRepository,
                            FileStorageService fileStorageService,
                            EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore,
                            ModelMapper modelMapper,
                            PromptCacheService promptCacheService,
                            PdfTextExtractorService pdfTextExtractorService,
                            @Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.documentoRepository = documentoRepository;
        this.empresaRepository = empresaRepository;
        this.fileStorageService = fileStorageService;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.modelMapper = modelMapper;
        this.promptCacheService = promptCacheService;
        this.pdfTextExtractorService = pdfTextExtractorService;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Construtor de compatibilidade para testes unitários.
     */
    public DocumentoService(DocumentoRepository documentoRepository,
                            FileStorageService fileStorageService,
                            EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore,
                            ModelMapper modelMapper,
                            PromptCacheService promptCacheService,
                            PdfTextExtractorService pdfTextExtractorService) {
        this(documentoRepository, null, fileStorageService, embeddingModel, embeddingStore,
                modelMapper, promptCacheService, pdfTextExtractorService, null);
    }

    public List<DocumentoResponseDTO> listarPorAssistente(UUID assistenteId) {
        UUID empresaId = obterEmpresaIdContexto();
        return documentoRepository.findByEmpresaIdAndAssistenteId(empresaId, assistenteId)
                .stream()
                .map(doc -> modelMapper.map(doc, DocumentoResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Realiza o upload e processamento de documento ou catálogo.
     * REGRA DE NEGÓCIO: Nenhum crédito ou token do usuário é descontado durante o upload ou ingestão!
     */
    public DocumentoResponseDTO fazerUploadEProcessar(MultipartFile file, UUID assistenteId) {
        UUID empresaId = obterEmpresaIdContexto();

        if (file == null || file.isEmpty()) {
            throw new BusinessException("O arquivo enviado está vazio.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessException("Nome do arquivo inválido.");
        }

        String extensao = extrairExtensao(originalFilename).toLowerCase();
        if (!isExtensaoPermitida(extensao)) {
            throw new BusinessException("Tipo de arquivo não suportado. Formatos aceitos: .pdf, .xlsx, .xls, .csv, .txt, .docx, .md");
        }

        // 1. Se já existir um documento com o mesmo nome original neste assistente, substitui apenas ele
        Optional<Documento> docMesmoNomeOpt = documentoRepository
                .findByEmpresaIdAndAssistenteIdAndNomeOriginalIgnoreCase(empresaId, assistenteId, originalFilename.trim());
        docMesmoNomeOpt.ifPresent(this::removerDocumentoFisicoEVetorial);

        // 2. Validar limites do plano da empresa (SIMPLES: 1 arq / 30MB | EMPRESARIAL: 5 arqs / 50MB)
        validarLimitesDoPlano(empresaId, file.getSize());

        // 3. Armazenar arquivo fisicamente e criar registro
        Documento documento = new Documento();
        documento.setEmpresaId(empresaId);
        documento.setAssistenteId(assistenteId);

        String nomeSalvo = UUID.randomUUID() + "_" + originalFilename.trim();
        String caminhoArquivo = fileStorageService.armazenar(file, empresaId, nomeSalvo);

        documento.setNomeOriginal(originalFilename.trim());
        documento.setNomeSalvo(nomeSalvo);
        documento.setCaminhoArquivo(caminhoArquivo);
        documento.setTipoConteudo(file.getContentType());
        documento.setTamanhoBytes(file.getSize());
        documento.setStatus(StatusDocumento.PROCESSANDO);
        documento.setMensagemErro(null);

        Documento docSalvo = documentoRepository.save(documento);

        // 4. Enfileirar no RabbitMQ (se habilitado e conectado) ou processar
        boolean enviadoFila = tentarPublicarNaFilaRabbitMq(docSalvo);

        if (!enviadoFila) {
            if (asyncFallbackEnabled) {
                CompletableFuture.runAsync(() -> processarDocumentoDoDisco(docSalvo.getId()));
            } else {
                executarIngestaoSincrona(docSalvo, file);
            }
        }

        Documento docFinal = documentoRepository.findById(docSalvo.getId()).orElse(docSalvo);
        return modelMapper.map(docFinal, DocumentoResponseDTO.class);
    }

    /**
     * Valida a quantidade máxima de arquivos e o limite total de megabytes de acordo com o plano da empresa:
     * - SIMPLES: máximo de 1 arquivo e limite de 30 MB por empresa.
     * - EMPRESARIAL: máximo de 5 arquivos e limite de 50 MB por empresa.
     */
    private void validarLimitesDoPlano(UUID empresaId, long tamanhoNovoArquivoBytes) {
        PlanoEmpresa plano = PlanoEmpresa.SIMPLES;
        if (empresaRepository != null) {
            Optional<Empresa> empresaOpt = empresaRepository.findById(empresaId);
            if (empresaOpt.isPresent() && empresaOpt.get().getPlano() != null) {
                plano = empresaOpt.get().getPlano();
            }
        }

        int maxArquivos = plano.getMaxArquivos();
        long maxBytesTotal = plano.getMaxBytesTotal();
        long maxMegabytes = plano.getMaxMegabytes();

        if (tamanhoNovoArquivoBytes > maxBytesTotal) {
            throw new BusinessException(String.format(
                    "O arquivo enviado (%.2f MB) excede o limite máximo do seu plano %s (%d MB).",
                    tamanhoNovoArquivoBytes / (1024.0 * 1024.0), plano.name(), maxMegabytes));
        }

        List<Documento> documentosDaEmpresa = documentoRepository.findByEmpresaId(empresaId);
        int quantidadeAtual = documentosDaEmpresa != null ? documentosDaEmpresa.size() : 0;

        if (quantidadeAtual >= maxArquivos) {
            if (plano == PlanoEmpresa.SIMPLES) {
                throw new BusinessException(
                        "Limite do plano SIMPLES atingido (máximo de 1 arquivo de até 30 MB por empresa). " +
                        "Exclua o arquivo atual ou faça upgrade para o plano EMPRESARIAL (até 5 arquivos e 50 MB).");
            } else {
                throw new BusinessException(String.format(
                        "Limite do plano %s atingido (máximo de %d arquivos por empresa). Exclua um arquivo antes de enviar outro.",
                        plano.name(), maxArquivos));
            }
        }

        long bytesAtuais = documentosDaEmpresa != null
                ? documentosDaEmpresa.stream().mapToLong(Documento::getTamanhoBytes).sum()
                : 0L;

        if (bytesAtuais + tamanhoNovoArquivoBytes > maxBytesTotal) {
            double mbDisponivel = Math.max(0.0, (maxBytesTotal - bytesAtuais) / (1024.0 * 1024.0));
            throw new BusinessException(String.format(
                    "Limite de armazenamento do plano %s (%d MB) excedido. Espaço disponível: %.2f MB.",
                    plano.name(), maxMegabytes, mbDisponivel));
        }
    }

    private boolean tentarPublicarNaFilaRabbitMq(Documento doc) {
        if (!rabbitMqEnabled || rabbitTemplate == null || doc.getId() == null) {
            return false;
        }
        try {
            DocumentoUploadEventDTO event = new DocumentoUploadEventDTO(
                    doc.getId(), doc.getEmpresaId(), doc.getAssistenteId(), doc.getCaminhoArquivo());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE_DOCUMENTO_RAG,
                    RabbitMqConfig.ROUTING_KEY_DOCUMENTO_RAG,
                    event);
            log.info("Documento {} enfileirado no RabbitMQ ({}) para ingestão RAG assíncrona.",
                    doc.getId(), RabbitMqConfig.QUEUE_DOCUMENTO_RAG);
            return true;
        } catch (Exception e) {
            log.warn("Broker RabbitMQ indisponível ({}). Executando processamento local resiliente.", e.getMessage());
            return false;
        }
    }

    /**
     * Consumidor da fila RabbitMQ para processamento assíncrono de PDFs e Catálogos.
     */
    @RabbitListener(queues = RabbitMqConfig.QUEUE_DOCUMENTO_RAG, autoStartup = "${app.rabbitmq.enabled:false}")
    public void consumirEventoUploadRabbitMq(DocumentoUploadEventDTO event) {
        if (event == null || event.getDocumentoId() == null) {
            return;
        }
        log.info("Consumindo evento RabbitMQ de ingestão RAG para o documento {}", event.getDocumentoId());
        processarDocumentoDoDisco(event.getDocumentoId());
    }

    public void processarDocumentoDoDisco(UUID documentoId) {
        Optional<Documento> docOpt = documentoRepository.findById(documentoId);
        if (docOpt.isEmpty()) {
            return;
        }
        Documento doc = docOpt.get();
        Path path = fileStorageService.carregar(doc.getCaminhoArquivo());
        try (InputStream is = Files.newInputStream(path)) {
            processarDocumentoRAG(doc, is);
            doc.setStatus(StatusDocumento.PROCESSADO);
            doc.setUpdatedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Erro ao processar documento {} em background: {}", doc.getId(), e.getMessage(), e);
            doc.setStatus(StatusDocumento.ERRO);
            doc.setMensagemErro(e.getMessage());
            doc.setUpdatedAt(LocalDateTime.now());
        }
        documentoRepository.save(doc);
        if (promptCacheService != null) {
            promptCacheService.invalidate(doc.getEmpresaId(), doc.getAssistenteId());
        }
    }

    private void executarIngestaoSincrona(Documento docSalvo, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            processarDocumentoRAG(docSalvo, inputStream);
            docSalvo.setStatus(StatusDocumento.PROCESSADO);
            docSalvo.setUpdatedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Erro ao processar embeddings do documento {}: {}", docSalvo.getId(), e.getMessage(), e);
            docSalvo.setStatus(StatusDocumento.ERRO);
            docSalvo.setMensagemErro(e.getMessage());
            docSalvo.setUpdatedAt(LocalDateTime.now());
        }

        documentoRepository.save(docSalvo);
        if (promptCacheService != null) {
            promptCacheService.invalidate(docSalvo.getEmpresaId(), docSalvo.getAssistenteId());
        }
    }

    private void removerDocumentoFisicoEVetorial(Documento docAntigo) {
        try {
            fileStorageService.excluir(docAntigo.getCaminhoArquivo());
        } catch (Exception e) {
            log.warn("Erro ao excluir ficheiro antigo do disco: {}", e.getMessage());
        }
        try {
            if (docAntigo.getId() != null) {
                embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("documento_id").isEqualTo(docAntigo.getId().toString()));
            }
        } catch (Exception e) {
            log.warn("Erro ao excluir vetores antigos: {}", e.getMessage());
        }
        documentoRepository.delete(docAntigo);
        documentoRepository.flush();
    }

    private void processarDocumentoRAG(Documento documento, InputStream inputStream) {
        String nomeOriginal = documento.getNomeOriginal() != null ? documento.getNomeOriginal() : "";
        String extensao = extrairExtensao(nomeOriginal).toLowerCase();
        String textoLimpo;

        if (".pdf".equals(extensao)) {
            log.info("Iniciando extração e limpeza de PDF com Apache PDFBox para documento {}", nomeOriginal);
            textoLimpo = pdfTextExtractorService.extrairELimparPdf(inputStream);
        } else if (".xlsx".equals(extensao) || ".xls".equals(extensao)) {
            log.info("Iniciando estruturação de planilha de catálogo Excel ({}) para RAG", nomeOriginal);
            textoLimpo = pdfTextExtractorService.extrairCatalogoExcel(inputStream);
        } else if (".csv".equals(extensao)) {
            log.info("Iniciando estruturação de catálogo CSV ({}) para RAG", nomeOriginal);
            textoLimpo = pdfTextExtractorService.extrairCatalogoCsv(inputStream);
        } else if (".txt".equals(extensao) || ".md".equals(extensao)) {
            log.info("Iniciando leitura e limpeza de arquivo de texto {} para RAG", nomeOriginal);
            try {
                String textoBruto = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                textoLimpo = pdfTextExtractorService.limparTextoBruto(textoBruto);
            } catch (Exception e) {
                throw new BusinessException("Erro ao ler o arquivo de texto: " + e.getMessage());
            }
        } else {
            log.info("Iniciando extração de texto com Apache Tika e limpeza para documento {}", nomeOriginal);
            ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
            Document tikaDoc = parser.parse(inputStream);
            textoLimpo = pdfTextExtractorService.limparTextoBruto(tikaDoc.text());
        }

        if (textoLimpo == null || textoLimpo.trim().isEmpty()) {
            throw new BusinessException("O documento não contém texto legível após a extração e limpeza.");
        }

        Document documentoLimpo = Document.from(textoLimpo);

        // Fatiamento recursivo (800 caracteres por pedaço com 100 caracteres de sobreposição)
        DocumentSplitter splitter = DocumentSplitters.recursive(800, 100);
        List<TextSegment> rawSegments = splitter.split(documentoLimpo);

        log.info("Documento/Catálogo limpo ({} caracteres) dividido em {} partes para vetorização local (Ollama) no pgvector",
                textoLimpo.length(), rawSegments.size());

        List<TextSegment> segmentsComMetadados = new ArrayList<>();
        for (TextSegment raw : rawSegments) {
            String trecho = raw.text() != null ? raw.text().trim() : "";
            if (trecho.isEmpty()) {
                continue;
            }
            Metadata metadata = new Metadata();
            metadata.put("empresa_id", documento.getEmpresaId().toString());
            if (documento.getAssistenteId() != null) {
                metadata.put("assistente_id", documento.getAssistenteId().toString());
            }
            if (documento.getId() != null) {
                metadata.put("documento_id", documento.getId().toString());
            }
            metadata.put("nome_arquivo", documento.getNomeOriginal());

            segmentsComMetadados.add(TextSegment.from(trecho, metadata));
        }

        // Geração de embeddings 100% local via Ollama (nomic-embed-text) - Custo Zero!
        log.info("Gerando embeddings localmente via Ollama (custo zero) e gravando {} vetores no pgvector...", segmentsComMetadados.size());
        Response<List<Embedding>> embeddingResponse = embeddingModel.embedAll(segmentsComMetadados);
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

        removerDocumentoFisicoEVetorial(documento);

        if (promptCacheService != null) {
            promptCacheService.invalidate(empresaId, documento.getAssistenteId());
        }
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
        return extensao.equals(".pdf")
                || extensao.equals(".xlsx")
                || extensao.equals(".xls")
                || extensao.equals(".csv")
                || extensao.equals(".docx")
                || extensao.equals(".doc")
                || extensao.equals(".txt")
                || extensao.equals(".md");
    }
}
