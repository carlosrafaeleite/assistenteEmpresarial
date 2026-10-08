package com.assistente.empresarial.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService {

    // Diretório base na máquina/servidor onde os ficheiros serão guardados
    private final Path raizUploads = Paths.get("upload");

    public LocalStorageService() {
        try {
            Files.createDirectories(raizUploads);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível inicializar a pasta de armazenamento local!", e);
        }
    }

    public String fazerUpload(UUID empresaId, MultipartFile file) {
        try {
            // Cria a pasta da empresa caso não exista: uploads-documentos/{empresaId}/
            Path pastaEmpresa = raizUploads.resolve(empresaId.toString());
            Files.createDirectories(pastaEmpresa);

            String nomeOriginal = file.getOriginalFilename();
            String nomeUnico = UUID.randomUUID() + "_" + (nomeOriginal != null ? nomeOriginal : "arquivo.pdf");

            Path caminhoCompleto = pastaEmpresa.resolve(nomeUnico);

            // Copia o ficheiro para o destino
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, caminhoCompleto, StandardCopyOption.REPLACE_EXISTING);
            }

            // Retorna o caminho relativo que será guardado na base de dados
            return empresaId.toString() + "/" + nomeUnico;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao guardar o ficheiro localmente: " + e.getMessage(), e);
        }
    }

    public InputStream descarregar(String caminhoRelativo) {
        try {
            Path caminhoCompleto = raizUploads.resolve(caminhoRelativo);
            return Files.newInputStream(caminhoCompleto);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler o ficheiro do disco: " + e.getMessage(), e);
        }
    }

    public void excluir(String caminhoRelativo) {
        try {
            Path caminhoCompleto = raizUploads.resolve(caminhoRelativo);
            Files.deleteIfExists(caminhoCompleto);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao eliminar o ficheiro do disco: " + e.getMessage(), e);
        }
    }
}