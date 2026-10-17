package com.assistente.empresarial.service.llm;

import com.assistente.empresarial.enuns.LlmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LlmRouterServiceTest {

    private OpenAiChatService openAiChatService;
    private GeminiChatService geminiChatService;
    private OllamaChatService ollamaChatService;
    private LlmRouterService routerService;

    @BeforeEach
    void setUp() {
        openAiChatService = new OpenAiChatService();
        ReflectionTestUtils.setField(openAiChatService, "chatModelName", "gpt-4o-mini");
        ReflectionTestUtils.setField(openAiChatService, "apiKey", "");

        geminiChatService = new GeminiChatService();
        ReflectionTestUtils.setField(geminiChatService, "chatModelName", "gemini-1.5-flash");
        ReflectionTestUtils.setField(geminiChatService, "apiKey", "");

        ollamaChatService = new OllamaChatService();
        ReflectionTestUtils.setField(ollamaChatService, "chatModelName", "llama3.2");
        ReflectionTestUtils.setField(ollamaChatService, "ollamaBaseUrl", "http://localhost:11434");

        routerService = new LlmRouterService(openAiChatService, geminiChatService, ollamaChatService);
        ReflectionTestUtils.setField(routerService, "defaultProviderProperty", "OPENAI");
        routerService.init();
    }

    @Test
    @DisplayName("Deve inicializar com OPENAI como ativo e fazer fallback para Ollama se OPENAI não tiver chave")
    void deveInicializarEFazerFallback() {
        assertEquals(LlmProvider.OPENAI, routerService.getActiveProvider());
        assertFalse(openAiChatService.isConfigured());
        assertTrue(ollamaChatService.isConfigured());

        // Como OpenAI não tem chave, o fallback deve escolher Ollama que está configurado
        LlmChatService activeService = routerService.getActiveService();
        assertEquals(LlmProvider.OLLAMA, activeService.getProvider());
    }

    @Test
    @DisplayName("Deve utilizar OpenAI quando a chave de API estiver presente")
    void deveUtilizarOpenAiQuandoConfigurado() {
        ReflectionTestUtils.setField(openAiChatService, "apiKey", "sk-test-fake-key-12345");
        assertTrue(openAiChatService.isConfigured());

        LlmChatService activeService = routerService.getActiveService();
        assertEquals(LlmProvider.OPENAI, activeService.getProvider());
        assertEquals("gpt-4o-mini", activeService.getModelName());
    }

    @Test
    @DisplayName("Deve permitir alternar para Gemini e utilizá-lo quando configurado")
    void deveAlternarParaGemini() {
        ReflectionTestUtils.setField(geminiChatService, "apiKey", "AIzaSyTestFakeKey123");
        assertTrue(geminiChatService.isConfigured());

        routerService.setActiveProvider(LlmProvider.GEMINI);
        assertEquals(LlmProvider.GEMINI, routerService.getActiveProvider());

        LlmChatService activeService = routerService.getActiveService();
        assertEquals(LlmProvider.GEMINI, activeService.getProvider());
        assertEquals("gemini-1.5-flash", activeService.getModelName());
    }

    @Test
    @DisplayName("Deve retornar status descritivo de todos os provedores")
    void deveRetornarStatusCompleto() {
        Map<String, Object> status = routerService.getStatus();
        assertNotNull(status);
        assertEquals("OPENAI", status.get("provedorAtivo"));
        assertTrue(status.containsKey("provedores"));

        @SuppressWarnings("unchecked")
        Map<String, Object> provedores = (Map<String, Object>) status.get("provedores");
        assertTrue(provedores.containsKey("OPENAI"));
        assertTrue(provedores.containsKey("GEMINI"));
        assertTrue(provedores.containsKey("OLLAMA"));
    }
}
