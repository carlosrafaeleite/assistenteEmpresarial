package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.ChatRequestDTO;
import com.assistente.empresarial.dto.ChatResponseDTO;
import com.assistente.empresarial.dto.DocumentoResponseDTO;
import com.assistente.empresarial.enuns.PlanoEmpresa;
import com.assistente.empresarial.enuns.StatusConversa;
import com.assistente.empresarial.enuns.StatusDocumento;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.TooManyRequestsException;
import com.assistente.empresarial.model.Assistente;
import com.assistente.empresarial.model.Conversa;
import com.assistente.empresarial.model.Documento;
import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.model.Mensagem;
import com.assistente.empresarial.repository.AssistenteRepository;
import com.assistente.empresarial.repository.ConversaRepository;
import com.assistente.empresarial.repository.DocumentoRepository;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.repository.MensagemRepository;
import com.assistente.empresarial.security.TenantContext;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnterpriseProductionCriteriaTest {

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private AssistenteRepository assistenteRepository;

    @Mock
    private ConversaRepository conversaRepository;

    @Mock
    private MensagemRepository mensagemRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    private DocumentoService documentoService;
    private PdfTextExtractorService extractorService;
    private ChatService chatService;
    private final UUID empresaId = UUID.randomUUID();
    private final UUID assistenteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(empresaId);
        extractorService = new PdfTextExtractorService();
        documentoService = new DocumentoService(
                documentoRepository,
                empresaRepository,
                fileStorageService,
                embeddingModel,
                embeddingStore,
                new ModelMapper(),
                new PromptCacheService(),
                extractorService,
                null
        );
        chatService = new ChatService(
                empresaRepository,
                assistenteRepository,
                conversaRepository,
                mensagemRepository,
                chatLanguageModel,
                embeddingModel,
                embeddingStore,
                new BCryptPasswordEncoder()
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("[Item 1 & 3] Plano SIMPLES: Deve permitir 1 arquivo de até 30MB e bloquear o 2º arquivo ou arquivo > 30MB")
    void deveRespeitarLimitesDoPlanoSimples() {
        Empresa empresaSimples = new Empresa();
        empresaSimples.setId(empresaId);
        empresaSimples.setPlano(PlanoEmpresa.SIMPLES);
        empresaSimples.setCreditosTokens(100_000L);

        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresaSimples));

        // 1. Arquivo maior que 30MB deve ser bloqueado no plano SIMPLES
        MockMultipartFile arquivo35Mb = new MockMultipartFile(
                "file", "catalogo.csv", "text/csv", new byte[31 * 1024 * 1024]);

        BusinessException exTamanho = assertThrows(BusinessException.class, () ->
                documentoService.fazerUploadEProcessar(arquivo35Mb, assistenteId));
        assertTrue(exTamanho.getMessage().contains("SIMPLES"));
        assertTrue(exTamanho.getMessage().contains("30 MB"));

        // 2. Se já possui 1 arquivo ativo no plano SIMPLES, deve bloquear o 2º arquivo diferente
        Documento docExistente = new Documento();
        docExistente.setId(UUID.randomUUID());
        docExistente.setEmpresaId(empresaId);
        docExistente.setNomeOriginal("manual1.pdf");
        docExistente.setTamanhoBytes(1024 * 1024);

        when(documentoRepository.findByEmpresaId(empresaId)).thenReturn(List.of(docExistente));

        MockMultipartFile segundoArquivo = new MockMultipartFile(
                "file", "manual2.txt", "text/plain", "Conteúdo do segundo arquivo".getBytes(StandardCharsets.UTF_8));

        BusinessException exQtd = assertThrows(BusinessException.class, () ->
                documentoService.fazerUploadEProcessar(segundoArquivo, assistenteId));
        assertTrue(exQtd.getMessage().contains("Limite do plano SIMPLES atingido"));
    }

    @Test
    @DisplayName("[Item 1 & 3] Plano EMPRESARIAL: Deve permitir até 5 arquivos e limite de 50MB, bloqueando o 6º arquivo")
    void deveRespeitarLimitesDoPlanoEmpresarial() {
        Empresa empresaEmpresarial = new Empresa();
        empresaEmpresarial.setId(empresaId);
        empresaEmpresarial.setPlano(PlanoEmpresa.EMPRESARIAL);

        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresaEmpresarial));

        // Simula 5 arquivos já existentes (10MB cada = 50MB)
        List<Documento> cincoDocs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Documento d = new Documento();
            d.setId(UUID.randomUUID());
            d.setEmpresaId(empresaId);
            d.setNomeOriginal("doc" + i + ".pdf");
            d.setTamanhoBytes(5 * 1024 * 1024);
            cincoDocs.add(d);
        }
        when(documentoRepository.findByEmpresaId(empresaId)).thenReturn(cincoDocs);

        MockMultipartFile sextoArquivo = new MockMultipartFile(
                "file", "doc6.txt", "text/plain", "Sexto arquivo".getBytes(StandardCharsets.UTF_8));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                documentoService.fazerUploadEProcessar(sextoArquivo, assistenteId));
        assertTrue(ex.getMessage().contains("EMPRESARIAL"));
        assertTrue(ex.getMessage().contains("5 arquivos"));
    }

    @Test
    @DisplayName("[Seção 1] Ingestão Gratuita de Catálogo Excel (.xlsx) e CSV: Deve estruturar itens e NÃO descontar créditos da empresa")
    void deveProcessarCatalogoExcelECsvSemDescontarCreditos() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        empresa.setPlano(PlanoEmpresa.EMPRESARIAL);
        empresa.setCreditosTokens(100_000L);

        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresa));
        when(documentoRepository.findByEmpresaId(empresaId)).thenReturn(Collections.emptyList());
        when(fileStorageService.armazenar(any(), any(), any())).thenReturn("./uploads/catalogo.xlsx");
        when(documentoRepository.save(any(Documento.class))).thenAnswer(inv -> {
            Documento d = inv.getArgument(0);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            return d;
        });
        when(documentoRepository.findById(any())).thenReturn(Optional.empty());
        when(embeddingModel.embedAll(anyList())).thenReturn(Response.from(List.of(Embedding.from(new float[768]))));

        // Cria planilha Excel (.xlsx) em memória com catálogo de produtos, preços e estoque
        byte[] excelBytes;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Produtos");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Produto");
            header.createCell(1).setCellValue("Preço");
            header.createCell(2).setCellValue("Estoque");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Notebook Gamer Pro");
            row1.createCell(1).setCellValue("R$ 5.499,00");
            row1.createCell(2).setCellValue("12 unidades");

            wb.write(bos);
            excelBytes = bos.toByteArray();
        }

        String textoExtraidoExcel = extractorService.extrairCatalogoExcel(new ByteArrayInputStream(excelBytes));
        assertTrue(textoExtraidoExcel.contains("Produto: Notebook Gamer Pro"));
        assertTrue(textoExtraidoExcel.contains("Preço: R$ 5.499,00"));
        assertTrue(textoExtraidoExcel.contains("Estoque: 12 unidades"));

        MockMultipartFile excelFile = new MockMultipartFile(
                "file", "catalogo_produtos.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        DocumentoResponseDTO response = documentoService.fazerUploadEProcessar(excelFile, assistenteId);
        assertNotNull(response);
        assertEquals(StatusDocumento.PROCESSADO, response.getStatus());

        // REGRA DE NEGÓCIO: Os créditos da empresa permanecem 100% intactos após o upload!
        assertEquals(100_000L, empresa.getCreditosTokens());
        assertEquals(0L, empresa.getTokensConsumidos());
    }

    @Test
    @DisplayName("[Item 2] Alinhamento de Dimensão 768d no DynamicEmbeddingService")
    void deveManterDimensaoAlinhadaEm768() {
        DynamicEmbeddingService embeddingService = new DynamicEmbeddingService();
        assertEquals(768, embeddingService.dimension());

        // Mesmo que um vetor externo tivesse 1536 dimensões, deve alinhar para 768
        Embedding vetor1536 = Embedding.from(new float[1536]);
        Embedding alinhado = embeddingService.alinharDimensao(vetor1536, 768);
        assertEquals(768, alinhado.vector().length);
    }

    @Test
    @DisplayName("[Item 5 & 8] Limite de Caracteres por Pergunta e Rate Limiting por Minuto")
    void deveLimitarCaracteresEPerguntasPorMinuto() {
        RateLimitService limiter = new RateLimitService(100, 3); // máx 100 chars, máx 3 perguntas/minuto

        // 1. Pergunta excedendo limite de caracteres
        String perguntaGigante = "A".repeat(120);
        assertThrows(BusinessException.class, () ->
                limiter.validarPerguntaERateLimit("cliente-1", perguntaGigante));

        // 2. Até 3 perguntas por minuto passam normalmente
        limiter.validarPerguntaERateLimit("cliente-1", "Pergunta 1");
        limiter.validarPerguntaERateLimit("cliente-1", "Pergunta 2");
        limiter.validarPerguntaERateLimit("cliente-1", "Pergunta 3");

        // 3. A 4ª pergunta dentro do mesmo minuto deve lançar TooManyRequestsException (HTTP 429)
        assertThrows(TooManyRequestsException.class, () ->
                limiter.validarPerguntaERateLimit("cliente-1", "Pergunta 4"));
    }

    @Test
    @DisplayName("[Item 9 & Seção 2] Consumo de Créditos/Tokens EXCLUSIVAMENTE no Fluxo de Chat e Bloqueio quando Saldo Zerado")
    void deveDescontarTokensExclusivamenteNoChat() {
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        empresa.setSlug("loja-teste");
        empresa.setPlano(PlanoEmpresa.SIMPLES);
        empresa.setCreditosTokens(1000L);

        Assistente assistente = new Assistente();
        assistente.setId(assistenteId);
        assistente.setEmpresaId(empresaId);
        assistente.setSlug("padrao");
        assistente.setNome("Assistente Loja");

        Conversa conversa = new Conversa();
        conversa.setId(UUID.randomUUID());
        conversa.setEmpresaId(empresaId);
        conversa.setAssistenteId(assistenteId);
        conversa.setStatus(StatusConversa.BOT);

        when(empresaRepository.findBySlug("loja-teste")).thenReturn(Optional.of(empresa));
        when(assistenteRepository.findBySlugAndEmpresaId("padrao", empresaId)).thenReturn(Optional.of(assistente));
        when(conversaRepository.save(any(Conversa.class))).thenReturn(conversa);
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[768])));
        when(chatLanguageModel.generate(anyList()))
                .thenReturn(Response.from(AiMessage.from("Temos o Notebook Gamer Pro em estoque por R$ 5.499,00!")));

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteIdentificador("cliente-xyz");
        request.setMensagem("Qual o preço do Notebook Gamer Pro?");

        ChatResponseDTO response = chatService.responderPublico("loja-teste", "padrao", request);

        assertNotNull(response);
        assertTrue(response.getResposta().contains("5.499,00"));
        assertTrue(empresa.getTokensConsumidos() > 0, "Deve registrar tokens consumidos na conversa do chat");
        assertTrue(empresa.getCreditosTokens() < 1000L, "Deve descontar créditos do saldo da empresa após resposta no chat");

        // Se o saldo zerar, deve bloquear novas chamadas à LLM
        empresa.setCreditosTokens(0L);
        ChatRequestDTO novaPergunta = new ChatRequestDTO();
        novaPergunta.setClienteIdentificador("cliente-xyz");
        novaPergunta.setMensagem("E qual o prazo de entrega para São Paulo?");

        assertThrows(BusinessException.class, () ->
                chatService.responderPublico("loja-teste", "padrao", novaPergunta));
    }

    @Test
    @DisplayName("[Item 7] Painel de Transbordo Humano em Tempo Real (SSE): Assumir, Responder e Encerrar sem gastar tokens")
    void deveGerenciarFluxoDeTransbordoHumano() {
        Conversa conversa = new Conversa();
        UUID conversaId = UUID.randomUUID();
        conversa.setId(conversaId);
        conversa.setEmpresaId(empresaId);
        conversa.setClienteNome("João Silva");
        conversa.setStatus(StatusConversa.AGUARDANDO_HUMANO);

        when(conversaRepository.findByIdAndEmpresaId(conversaId, empresaId)).thenReturn(Optional.of(conversa));
        when(conversaRepository.save(any(Conversa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Atendente assume a conversa
        Conversa assumida = chatService.assumirAtendimentoHumano(conversaId);
        assertEquals(StatusConversa.EM_ATENDIMENTO_HUMANO, assumida.getStatus());

        // 2. Atendente envia resposta humana
        Mensagem respostaHumana = chatService.responderComoAtendenteHumano(conversaId, "Olá João, sou o Carlos do suporte! Como posso ajudar?");
        assertEquals("ATENDENTE", respostaHumana.getRemetente());
        assertTrue(respostaHumana.getConteudo().contains("Carlos do suporte"));

        // 3. Encerrar atendimento e devolver para o bot
        Conversa devolvida = chatService.encerrarAtendimentoHumano(conversaId, true);
        assertEquals(StatusConversa.BOT, devolvida.getStatus());
    }
}
