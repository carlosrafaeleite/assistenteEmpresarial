package com.assistente.empresarial.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PdfTextExtractorServiceTest {

    private final PdfTextExtractorService extractorService = new PdfTextExtractorService();

    @Test
    @DisplayName("Deve limpar quebras de linha desnecessárias, hifenização e múltiplos espaços")
    void deveLimparQuebrasDeLinhaEMultiplosEspacos() {
        String textoSujo = "A nossa política de atendi-\nmento funciona de segunda\n" +
                "a sexta-feira, das   08h   às    18h.\n\n\n\n" +
                "Página 1 de 5\n" +
                "- Suporte técnico via chat\n" +
                "- Atendimento comercial";

        String textoLimpo = extractorService.limparTextoBruto(textoSujo);

        assertFalse(textoLimpo.contains("atendi-\nmento"), "Deve unir palavras hifenizadas na quebra de linha");
        assertTrue(textoLimpo.contains("atendimento funciona de segunda a sexta-feira, das 08h às 18h."),
                "Deve remover quebras de linha no meio da frase e múltiplos espaços");
        assertFalse(textoLimpo.contains("Página 1 de 5"), "Deve remover linha isolada de numeração de página");
        assertTrue(textoLimpo.contains("- Suporte técnico via chat\n- Atendimento comercial"),
                "Deve preservar quebras de linha em listas de tópicos");
    }

    @Test
    @DisplayName("Deve extrair texto de PDF com Apache PDFBox e remover cabeçalhos/rodapés repetidos")
    void deveExtrairPdfERemoverRodapesRepetidos() throws Exception {
        byte[] pdfBytes;
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Página 1
            PDPage page1 = new PDPage();
            doc.addPage(page1);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page1)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 750);
                cs.showText("Manual Oficial Loja Exemplo LTDA - CNPJ 00.000.000/0001-00");
                cs.newLineAtOffset(0, -40);
                cs.showText("O prazo de troca dos produtos comprados na loja");
                cs.newLineAtOffset(0, -20);
                cs.showText("e de ate 30 dias corridos mediante apresentacao da nota.");
                cs.newLineAtOffset(0, -500);
                cs.showText("Rodape Corporativo Repetido - Todos os direitos reservados");
                cs.newLineAtOffset(0, -20);
                cs.showText("Pagina 1 de 2");
                cs.endText();
            }

            // Página 2
            PDPage page2 = new PDPage();
            doc.addPage(page2);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page2)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 750);
                cs.showText("Manual Oficial Loja Exemplo LTDA - CNPJ 00.000.000/0001-00");
                cs.newLineAtOffset(0, -40);
                cs.showText("Entregas expressas sao realizadas em ate 24 horas uteis.");
                cs.newLineAtOffset(0, -500);
                cs.showText("Rodape Corporativo Repetido - Todos os direitos reservados");
                cs.newLineAtOffset(0, -20);
                cs.showText("Pagina 2 de 2");
                cs.endText();
            }

            doc.save(out);
            pdfBytes = out.toByteArray();
        }

        String textoExtraido = extractorService.extrairELimparPdf(new ByteArrayInputStream(pdfBytes));

        assertNotNull(textoExtraido);
        assertTrue(textoExtraido.contains("O prazo de troca dos produtos comprados na loja e de ate 30 dias corridos"));
        assertTrue(textoExtraido.contains("Entregas expressas sao realizadas em ate 24 horas uteis."));
        assertFalse(textoExtraido.contains("Rodape Corporativo Repetido"), "Rodapé repetido deve ser filtrado");
        assertFalse(textoExtraido.contains("Manual Oficial Loja Exemplo LTDA"), "Cabeçalho repetido deve ser filtrado");
        assertFalse(textoExtraido.contains("Pagina 1 de 2"), "Numeração de página deve ser filtrada");
    }
}
