package com.assistente.empresarial.service;

import com.assistente.empresarial.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
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
public class FileStorageService {

    private final Path rootLocation;

    public FileStorageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new BusinessException("Não foi possível inicializar o diretório de armazenamento de arquivos: " + e.getMessage());
        }
    }

    public String armazenar(MultipartFile file, UUID empresaId, String nomeSalvo) {
        if (file.isEmpty()) {
            throw new BusinessException("Não é permitido enviar arquivo vazio.");
        }

        try {
            Path tenantDir = this.rootLocation.resolve(empresaId.toString()).normalize();
            if (!Files.exists(tenantDir)) {
                Files.createDirectories(tenantDir);
            }

            Path destino = tenantDir.resolve(nomeSalvo).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
            }

            return destino.toString();
        } catch (IOException e) {
            throw new BusinessException("Falha ao gravar arquivo em disco: " + e.getMessage());
        }
    }

    public Path carregar(String caminhoArquivo) {
        return Paths.get(caminhoArquivo);
    }

    public void excluir(String caminhoArquivo) {
        try {
            Path path = Paths.get(caminhoArquivo);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Log apenas
        }
    }
}
