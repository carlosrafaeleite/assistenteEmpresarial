package com.assistente.empresarial.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class AppConfig {

    @Bean
    public CommandLineRunner legacyColumnMigrationRunner(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("UPDATE empresa SET plano = 'SIMPLES' WHERE plano IS NULL");
                stmt.executeUpdate("UPDATE empresa SET creditos_tokens = 100000 WHERE creditos_tokens IS NULL");
                stmt.executeUpdate("UPDATE empresa SET tokens_consumidos = 0 WHERE tokens_consumidos IS NULL");
                stmt.execute("ALTER TABLE IF EXISTS conversas DROP CONSTRAINT IF EXISTS conversas_status_check");
            } catch (Exception ignored) {
                // Ignora caso seja banco em memória de teste ou tabela ainda vazia
            }
        };
    }

    @Bean
    public ModelMapper modelmapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(org.modelmapper.convention.MatchingStrategies.STRICT);
        return modelMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
