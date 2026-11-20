package com.assistente.empresarial.service;

import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.model.Conversa;
import com.assistente.empresarial.model.Empresa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço de Integração Gratuita com Slack via Incoming Webhooks (Opção 1).
 * - Dispara notificações instantâneas no canal do Slack da empresa sempre que um cliente
 *   solicita atendimento humano ou envia mensagem na fila de transbordo.
 * - Não consome créditos/tokens da empresa.
 */
@Service
public class SlackWebhookService {

    private static final Logger log = LoggerFactory.getLogger(SlackWebhookService.class);

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendBaseUrl = "http://localhost:5173";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Dispara alerta assíncrono para o Slack da empresa quando um cliente solicita atendente humano.
     */
    public void notificarTransbordoAssincrono(Empresa empresa, Conversa conversa, String mensagemCliente) {
        if (empresa == null || empresa.getSlackWebhookUrl() == null || empresa.getSlackWebhookUrl().trim().isEmpty()) {
            return;
        }

        String webhookUrl = empresa.getSlackWebhookUrl().trim();
        String nomeEmpresa = empresa.getNome() != null ? empresa.getNome() : "Sua Empresa";
        String nomeCliente = (conversa != null && conversa.getClienteNome() != null && !conversa.getClienteNome().isBlank())
                ? conversa.getClienteNome()
                : "Visitante no Site";
        String contatoCliente = (conversa != null && conversa.getClienteEmail() != null && !conversa.getClienteEmail().isBlank())
                ? conversa.getClienteEmail().trim()
                : null;
        String linhaContato = contatoCliente != null
                ? "• *Contato / E-mail:* " + escaparJson(contatoCliente) + "\n"
                : "";
        String conversaIdStr = conversa != null && conversa.getId() != null ? conversa.getId().toString() : "";
        String linkAtendimento = frontendBaseUrl + "/dashboard/atendimento"
                + (!conversaIdStr.isEmpty() ? "?conversaId=" + conversaIdStr : "");

        String textoFormatado = String.format(
                ":rotating_light: *Novo Cliente Aguardando Atendimento Humano!* (%s)\n" +
                "• *Cliente:* %s\n" +
                "%s" +
                "• *Última mensagem:* _\"%s\"_\n" +
                "• *Protocolo:* `%s`\n" +
                ":point_right: *<%s|Clique aqui para abrir o Painel e Assumir a Conversa>*",
                escaparJson(nomeEmpresa),
                escaparJson(nomeCliente),
                linhaContato,
                escaparJson(mensagemCliente != null ? mensagemCliente : "Solicitou falar com atendente"),
                escaparJson(conversaIdStr.length() >= 8 ? conversaIdStr.substring(0, 8) : conversaIdStr),
                linkAtendimento
        );

        CompletableFuture.runAsync(() -> enviarPayloadSlack(webhookUrl, textoFormatado));
    }

    /**
     * Envia uma mensagem de teste síncrona para validar se o Incoming Webhook do Slack está funcionando.
     */
    public boolean enviarTesteWebhook(String webhookUrl, String nomeEmpresa) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            throw new BusinessException("Informe a URL do Incoming Webhook do Slack antes de testar.");
        }
        String urlLimpa = webhookUrl.trim();
        if (!urlLimpa.startsWith("http://") && !urlLimpa.startsWith("https://")) {
            throw new BusinessException("A URL do Webhook do Slack deve iniciar com https://");
        }

        String linkPainel = frontendBaseUrl + "/dashboard/atendimento";
        String textoTeste = String.format(
                ":white_check_mark: *Integração Slack Ativa — %s*\n" +
                "O seu canal está conectado ao *Painel de Transbordo Humano*! Sempre que um cliente pedir atendimento humano no chat, você receberá um alerta aqui com o link direto: <%s|Abrir Painel de Atendimento>.",
                escaparJson(nomeEmpresa != null ? nomeEmpresa : "Assistente Empresarial"),
                linkPainel
        );

        return enviarPayloadSlack(urlLimpa, textoTeste);
    }

    private boolean enviarPayloadSlack(String webhookUrl, String textoSlack) {
        try {
            String jsonBody = "{\"text\":\"" + textoSlack + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Notificação enviada com sucesso para o Slack (HTTP {}).", response.statusCode());
                return true;
            } else {
                log.warn("Slack retornou status HTTP {} ao enviar webhook: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.warn("Falha ao enviar notificação para o Webhook do Slack: {}", e.getMessage());
            return false;
        }
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
    }
}
