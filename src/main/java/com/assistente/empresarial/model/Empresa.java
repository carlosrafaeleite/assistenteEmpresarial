package com.assistente.empresarial.model;

import com.assistente.empresarial.enuns.PlanoEmpresa;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "empresa")
public class Empresa {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID id;

	@Column(nullable = false)
	private String nome;

	@Column(name = "razao_social")
	private String razaoSocial;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false, unique = true)
	private String slug;

	@Column(nullable = false)
	private boolean status = true;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at")
	private LocalDateTime updatedAt = LocalDateTime.now();

	@Column(name = "limite_assistentes", nullable = false)
	private int limiteAssistentes = 5; // Padrão do plano inicial

	@Column(name = "nome_bot")
	private String nomeBot = "Assistente IA";

	@Enumerated(EnumType.STRING)
	@Column(name = "plano", columnDefinition = "varchar(50) default 'SIMPLES'")
	private PlanoEmpresa plano = PlanoEmpresa.SIMPLES;

	@Column(name = "creditos_tokens", columnDefinition = "bigint default 100000")
	private Long creditosTokens = PlanoEmpresa.SIMPLES.getCreditosIniciais();

	@Column(name = "tokens_consumidos", columnDefinition = "bigint default 0")
	private Long tokensConsumidos = 0L;

	@Column(name = "slack_webhook_url", length = 500)
	private String slackWebhookUrl;

	// Construtores, Getters e Setters
	public Empresa() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getRazaoSocial() {
		return razaoSocial;
	}

	public void setRazaoSocial(String razaoSocial) {
		this.razaoSocial = razaoSocial;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public boolean isStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public int getLimiteAssistentes() {
		return limiteAssistentes;
	}

	public void setLimiteAssistentes(int limiteAssistentes) {
		this.limiteAssistentes = limiteAssistentes;
	}

	public String getNomeBot() {
		return nomeBot;
	}

	public void setNomeBot(String nomeBot) {
		this.nomeBot = nomeBot;
	}

	public PlanoEmpresa getPlano() {
		return plano != null ? plano : PlanoEmpresa.SIMPLES;
	}

	public void setPlano(PlanoEmpresa plano) {
		this.plano = plano != null ? plano : PlanoEmpresa.SIMPLES;
	}

	public long getCreditosTokens() {
		return creditosTokens != null ? creditosTokens : PlanoEmpresa.SIMPLES.getCreditosIniciais();
	}

	public void setCreditosTokens(long creditosTokens) {
		this.creditosTokens = Math.max(0L, creditosTokens);
	}

	public long getTokensConsumidos() {
		return tokensConsumidos != null ? tokensConsumidos : 0L;
	}

	public void setTokensConsumidos(long tokensConsumidos) {
		this.tokensConsumidos = Math.max(0L, tokensConsumidos);
	}

	public String getSlackWebhookUrl() {
		return slackWebhookUrl;
	}

	public void setSlackWebhookUrl(String slackWebhookUrl) {
		this.slackWebhookUrl = slackWebhookUrl;
	}

	public int getMaxArquivosPermitidos() {
		return getPlano().getMaxArquivos();
	}

	public long getLimiteArmazenamentoBytes() {
		return getPlano().getMaxBytesTotal();
	}

	/**
	 * Debita tokens consumidos exclusivamente durante o fluxo de conversação no chat.
	 */
	public void debitarTokensChat(long tokens) {
		if (tokens <= 0) {
			return;
		}
		this.tokensConsumidos = getTokensConsumidos() + tokens;
		this.creditosTokens = Math.max(0L, getCreditosTokens() - tokens);
		this.updatedAt = LocalDateTime.now();
	}
}
