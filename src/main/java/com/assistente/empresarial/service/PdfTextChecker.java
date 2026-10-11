package com.assistente.empresarial.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

public class PdfTextChecker {

    public static boolean hasTextLayer(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();

            // Analisa apenas as primeiras páginas para ser rápido (ex: até a página 3)
            stripper.setEndPage(Math.min(3, document.getNumberOfPages()));
            String extractedText = stripper.getText(document);

            // Se o texto extraído for muito curto, consideramos que é um PDF escaneado (imagem)
            return extractedText != null && extractedText.trim().length() > 100;

        } catch (IOException e) {
            // Tratar exceção de arquivo corrompido
            return false;
        }
    }
}
