package com.assistente.empresarial.service;

import com.assistente.empresarial.exception.BusinessException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Serviço especializado na extração de texto de arquivos PDF utilizando Apache PDFBox,
 * estruturação de catálogos de produtos em planilhas Excel (.xlsx, .xls) e CSV (.csv),
 * e higienização textual (limpeza de quebras de linha desnecessárias, múltiplos espaços
 * e cabeçalhos/rodapés repetitivos) antes da geração local de embeddings via Ollama para o pgvector (RAG).
 */
@Service
public class PdfTextExtractorService {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractorService.class);

    // Padrões comuns de numeração de página isolada em rodapés/cabeçalhos
    private static final Pattern PADRAO_NUMERO_PAGINA = Pattern.compile(
            "(?i)^\\s*(?:p[aá]gina|p[aá]g\\.?|page|pg\\.?)?\\s*[-–—]?\\s*\\d+\\s*(?:(?:de|of|/)\\s*\\d+)?\\s*[-–—]?\\s*$"
    );

    // Hifenização de fim de linha (ex: "desenvol-\nvimento" -> "desenvolvimento")
    private static final Pattern PADRAO_HIFEN_QUEBRA_LINHA = Pattern.compile(
            "([\\p{L}])-\\s*\\n\\s*([\\p{Ll}])"
    );

    // Marcadores de início de novo bloco (listas, tópicos, títulos numerados) que devem manter quebra de linha
    private static final Pattern PADRAO_INICIO_TOPICO = Pattern.compile(
            "^(\\s*[-•*▪▸]|\\s*\\d+[.)]|\\s*[a-zA-Z][.)]|\\s*(?:Art\\.|Cláusula|Seção|Capítulo| Cap\\.)|#).*"
    );

    /**
     * Extrai o texto de um PDF com Apache PDFBox página a página, filtra cabeçalhos e rodapés
     * repetidos e aplica a rotina de limpeza textual para maximizar a qualidade do RAG.
     */
    public String extrairELimparPdf(InputStream inputStream) {
        try (PDDocument document = PDDocument.load(inputStream)) {
            if (document.isEncrypted()) {
                throw new BusinessException("O arquivo PDF está protegido por senha e não pode ser processado.");
            }

            int totalPaginas = document.getNumberOfPages();
            if (totalPaginas == 0) {
                throw new BusinessException("O arquivo PDF não possui páginas.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            List<List<String>> paginasLinhas = new ArrayList<>(totalPaginas);
            for (int pagina = 1; pagina <= totalPaginas; pagina++) {
                stripper.setStartPage(pagina);
                stripper.setEndPage(pagina);
                String textoPagina = stripper.getText(document);
                if (textoPagina != null && !textoPagina.isBlank()) {
                    List<String> linhas = Arrays.stream(textoPagina.replace("\r\n", "\n").replace("\r", "\n").split("\n"))
                            .map(String::trim)
                            .collect(Collectors.toList());
                    paginasLinhas.add(linhas);
                }
            }

            if (paginasLinhas.isEmpty()) {
                throw new BusinessException("O PDF não contém camada de texto legível (possível PDF escaneado como imagem).");
            }

            // Identifica cabeçalhos e rodapés repetitivos entre páginas
            Set<String> linhasRepetidasRodapeCabecalho = identificarCabecalhosERodapesRepetidos(paginasLinhas);
            if (!linhasRepetidasRodapeCabecalho.isEmpty()) {
                log.info("Foram identificados {} padrões de cabeçalho/rodapé repetidos no PDF para remoção.",
                        linhasRepetidasRodapeCabecalho.size());
            }

            // Reconstrói o documento filtrando os rodapés/cabeçalhos e números de página
            StringBuilder textoBrutoSemRodape = new StringBuilder();
            for (List<String> linhasPagina : paginasLinhas) {
                int totalLinhas = linhasPagina.size();
                for (int i = 0; i < totalLinhas; i++) {
                    String linha = linhasPagina.get(i);
                    if (linha.isEmpty()) {
                        textoBrutoSemRodape.append("\n");
                        continue;
                    }

                    boolean naBordaDaPagina = (i < 3) || (i >= totalLinhas - 3);
                    if (naBordaDaPagina && PADRAO_NUMERO_PAGINA.matcher(linha).matches()) {
                        continue;
                    }

                    String assinaturaLinha = normalizarAssinaturaLinha(linha);
                    if (naBordaDaPagina && linhasRepetidasRodapeCabecalho.contains(assinaturaLinha)) {
                        continue;
                    }

                    textoBrutoSemRodape.append(linha).append("\n");
                }
                textoBrutoSemRodape.append("\n");
            }

            String textoLimpo = limparTextoBruto(textoBrutoSemRodape.toString());
            if (textoLimpo.isEmpty()) {
                throw new BusinessException("O documento PDF não contém texto útil após a limpeza.");
            }

            return textoLimpo;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("Falha ao extrair texto do PDF com Apache PDFBox: {}", e.getMessage(), e);
            throw new BusinessException("Erro ao ler o arquivo PDF. Verifique se o arquivo não está corrompido.");
        }
    }

    /**
     * Estrutura dados de planilhas Excel (.xlsx, .xls) de catálogos (produtos, preços, estoque)
     * associando cada célula ao cabeçalho da respectiva coluna para busca semântica de alta precisão.
     */
    public String extrairCatalogoExcel(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter(new Locale("pt", "BR"));
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            StringBuilder sb = new StringBuilder();

            int totalSheets = workbook.getNumberOfSheets();
            for (int s = 0; s < totalSheets; s++) {
                Sheet sheet = workbook.getSheetAt(s);
                if (sheet == null) continue;

                String sheetName = sheet.getSheetName() != null ? sheet.getSheetName().trim() : "Catálogo";
                List<String> headers = new ArrayList<>();
                boolean headerCaptured = false;
                int itemIndex = 1;

                for (Row row : sheet) {
                    if (row == null) continue;
                    int lastCellNum = row.getLastCellNum();
                    if (lastCellNum <= 0) continue;

                    List<String> valoresLinha = new ArrayList<>();
                    boolean linhaNaoVazia = false;

                    for (int c = 0; c < lastCellNum; c++) {
                        Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        String val = cell != null ? formatter.formatCellValue(cell, evaluator).trim() : "";
                        val = val.replaceAll("\\s+", " ");
                        if (!val.isEmpty()) {
                            linhaNaoVazia = true;
                        }
                        valoresLinha.add(val);
                    }

                    if (!linhaNaoVazia) {
                        continue;
                    }

                    if (!headerCaptured) {
                        for (int i = 0; i < valoresLinha.size(); i++) {
                            String h = valoresLinha.get(i);
                            headers.add(h.isEmpty() ? "Coluna_" + (i + 1) : h);
                        }
                        headerCaptured = true;
                        continue;
                    }

                    List<String> pares = new ArrayList<>();
                    for (int i = 0; i < valoresLinha.size(); i++) {
                        String val = valoresLinha.get(i);
                        if (!val.isEmpty()) {
                            String nomeColuna = i < headers.size() ? headers.get(i) : "Coluna_" + (i + 1);
                            pares.add(nomeColuna + ": " + val);
                        }
                    }

                    if (!pares.isEmpty()) {
                        sb.append(String.format("[Item #%d - %s] %s\n",
                                itemIndex++, sheetName, String.join(" | ", pares)));
                    }
                }
                sb.append("\n");
            }

            String resultado = sb.toString().trim();
            if (resultado.isEmpty()) {
                throw new BusinessException("A planilha Excel está vazia ou não contém dados legíveis.");
            }
            return resultado;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha ao estruturar planilha Excel: {}", e.getMessage(), e);
            throw new BusinessException("Erro ao processar planilha Excel: " + e.getMessage());
        }
    }

    /**
     * Estrutura arquivos CSV de catálogos (produtos, preços, estoque) mapeando cada coluna ao seu cabeçalho.
     */
    public String extrairCatalogoCsv(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String> linhas = reader.lines()
                    .map(l -> l.replaceAll("[\\uFEFF]", "").trim())
                    .filter(l -> !l.isEmpty())
                    .collect(Collectors.toList());

            if (linhas.isEmpty()) {
                throw new BusinessException("O arquivo CSV está vazio.");
            }

            if (linhas.size() == 1) {
                return limparTextoBruto(linhas.get(0));
            }

            String primeiraLinha = linhas.get(0);
            String delimitador = detectarDelimitadorCsv(primeiraLinha);
            String[] headers = separarLinhaCsv(primeiraLinha, delimitador);

            StringBuilder sb = new StringBuilder();
            int itemIndex = 1;

            for (int i = 1; i < linhas.size(); i++) {
                String[] valores = separarLinhaCsv(linhas.get(i), delimitador);
                List<String> pares = new ArrayList<>();
                for (int c = 0; c < valores.length; c++) {
                    String val = valores[c].trim();
                    if (!val.isEmpty()) {
                        String col = (c < headers.length && !headers[c].trim().isEmpty())
                                ? headers[c].trim()
                                : "Campo_" + (c + 1);
                        pares.add(col + ": " + val);
                    }
                }
                if (!pares.isEmpty()) {
                    sb.append(String.format("[Item #%d] %s\n", itemIndex++, String.join(" | ", pares)));
                }
            }

            String estruturado = sb.toString().trim();
            return estruturado.isEmpty() ? limparTextoBruto(String.join("\n", linhas)) : estruturado;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("Erro ao ler arquivo CSV: " + e.getMessage());
        }
    }

    private String detectarDelimitadorCsv(String cabecalho) {
        int pontoVirgula = contarOcorrencias(cabecalho, ';');
        int virgula = contarOcorrencias(cabecalho, ',');
        int tab = contarOcorrencias(cabecalho, '\t');
        if (pontoVirgula >= virgula && pontoVirgula >= tab && pontoVirgula > 0) {
            return ";";
        }
        if (tab > virgula && tab > 0) {
            return "\t";
        }
        return ",";
    }

    private int contarOcorrencias(String texto, char alvo) {
        int count = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == alvo) count++;
        }
        return count;
    }

    private String[] separarLinhaCsv(String linha, String delimitador) {
        String[] partes = linha.split(Pattern.quote(delimitador), -1);
        for (int i = 0; i < partes.length; i++) {
            String p = partes[i].trim();
            if (p.length() >= 2 && ((p.startsWith("\"") && p.endsWith("\"")) || (p.startsWith("'") && p.endsWith("'")))) {
                p = p.substring(1, p.length() - 1).trim();
            }
            partes[i] = p;
        }
        return partes;
    }

    /**
     * Rotina de limpeza textual para RAG:
     * - Remove caracteres de controle invisíveis
     * - Corrige palavras separadas por hífen na quebra de linha
     * - Remove quebras de linha desnecessárias no meio de frases
     * - Remove espaços múltiplos e excesso de linhas em branco
     */
    public String limparTextoBruto(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }

        // 1. Normaliza quebras de linha e remove caracteres de controle invisíveis (exceto \n e \t)
        String processado = texto
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\u200B-\\u200D\\uFEFF]", "");

        // 2. Une palavras hifenizadas na quebra de linha (ex: "atendi-\nmento" -> "atendimento")
        processado = PADRAO_HIFEN_QUEBRA_LINHA.matcher(processado).replaceAll("$1$2");

        // 3. Remove linhas isoladas que são apenas numeração de página
        String[] linhas = processado.split("\n");
        StringBuilder sbParagrafos = new StringBuilder();

        for (int i = 0; i < linhas.length; i++) {
            // Limpa espaços múltiplos dentro da própria linha
            String linhaAtual = linhas[i].replaceAll("[ \\t\\x0B\\f]+", " ").trim();

            if (PADRAO_NUMERO_PAGINA.matcher(linhaAtual).matches()) {
                continue;
            }

            if (linhaAtual.isEmpty()) {
                sbParagrafos.append("\n\n");
                continue;
            }

            if (sbParagrafos.length() == 0 || sbParagrafos.toString().endsWith("\n\n")) {
                sbParagrafos.append(linhaAtual);
            } else {
                // Verifica se a quebra de linha era desnecessária (frase continuando na linha seguinte)
                char ultimoChar = sbParagrafos.charAt(sbParagrafos.length() - 1);
                boolean terminouPontuacaoForte = ultimoChar == '.' || ultimoChar == '!' || ultimoChar == '?'
                        || ultimoChar == ':' || ultimoChar == ';';
                boolean proximaLinhaEhTopico = PADRAO_INICIO_TOPICO.matcher(linhaAtual).matches();

                if (!terminouPontuacaoForte && !proximaLinhaEhTopico) {
                    // Junta com espaço simples (remove quebra de linha desnecessária no meio do parágrafo)
                    sbParagrafos.append(" ").append(linhaAtual);
                } else {
                    sbParagrafos.append("\n").append(linhaAtual);
                }
            }
        }

        // 4. Colapsa múltiplos espaços residuais e limita quebras de linha a no máximo 2 consecutivas
        return sbParagrafos.toString()
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * Analisa as 3 primeiras e 3 últimas linhas não-vazias de cada página para detectar
     * cabeçalhos ou rodapés repetidos (como CNPJ, endereço, "Documento Confidencial", etc.).
     */
    private Set<String> identificarCabecalhosERodapesRepetidos(List<List<String>> paginasLinhas) {
        int totalPaginas = paginasLinhas.size();
        if (totalPaginas < 2) {
            return Collections.emptySet();
        }

        Map<String, Integer> frequenciaBordas = new HashMap<>();

        for (List<String> linhas : paginasLinhas) {
            Set<String> assinaturasNaPagina = new HashSet<>();
            int totalLinhas = linhas.size();

            for (int i = 0; i < totalLinhas; i++) {
                boolean naBorda = (i < 3) || (i >= totalLinhas - 3);
                if (!naBorda) {
                    continue;
                }
                String linha = linhas.get(i);
                if (linha.length() >= 4) {
                    assinaturasNaPagina.add(normalizarAssinaturaLinha(linha));
                }
            }

            for (String assinatura : assinaturasNaPagina) {
                frequenciaBordas.merge(assinatura, 1, Integer::sum);
            }
        }

        // Se tem 2 páginas, precisa repetir nas 2; se tem 3 ou mais, repetir em pelo menos 60% das páginas (mínimo 2)
        int limiarRepeticao = Math.max(2, (int) Math.ceil(totalPaginas * 0.6));

        return frequenciaBordas.entrySet().stream()
                .filter(entry -> entry.getValue() >= limiarRepeticao)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Normaliza dígitos para '#' e espaços para comparar linhas de rodapé/cabeçalho
     * que variam apenas no número da página (ex: "Empresa X - Página 1" == "Empresa X - Página 2").
     */
    private String normalizarAssinaturaLinha(String linha) {
        return linha.toLowerCase(Locale.ROOT)
                .replaceAll("\\d+", "#")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
