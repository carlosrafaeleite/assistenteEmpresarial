package com.assistente.empresarial.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assistente")
public class Assistente {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(nullable = false)
	private String nome;

	@Column(columnDefinition = "TEXT")
	private String descricao;

	@Column(name = "prompt_sistema", columnDefinition = "TEXT")
	private String promptSistema;

	@Column(name = "cor_primaria")
	private String corPrimaria = "#2563EB";

	@Column(name = "cor_secundaria")
	private String corSecundaria = "#1E40AF";

	@Column(name = "avatar_url")
	private String avatarUrl;

	@Column(name = "mensagem_boas_vindas", columnDefinition = "TEXT")
	private String mensagemBoasVindas = "Olá! Como posso ajudar você hoje?";

	@Column(name = "tom_voz")
	private String tomVoz = "AMIGAVEL"; // AMIGAVEL, FORMAL, TECNICO

	@Column(nullable = false)
	private boolean ativo = true;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at")
	private LocalDateTime updatedAt = LocalDateTime.now();

	@Column(nullable = false, unique = true) // Ou unique por empresa, dependendo da tua regra
	private String slug;

	public Assistente() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getEmpresaId() {
		return empresaId;
	}

	public void setEmpresaId(UUID empresaId) {
		this.empresaId = empresaId;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public String getPromptSistema() {
		return promptSistema;
	}

	public void setPromptSistema(String promptSistema) {
		this.promptSistema = promptSistema;
	}

	public String getCorPrimaria() {
		return corPrimaria;
	}

	public void setCorPrimaria(String corPrimaria) {
		this.corPrimaria = corPrimaria;
	}

	public String getCorSecundaria() {
		return corSecundaria;
	}

	public void setCorSecundaria(String corSecundaria) {
		this.corSecundaria = corSecundaria;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public String getMensagemBoasVindas() {
		return mensagemBoasVindas;
	}

	public void setMensagemBoasVindas(String mensagemBoasVindas) {
		this.mensagemBoasVindas = mensagemBoasVindas;
	}

	public String getTomVoz() {
		return tomVoz;
	}

	public void setTomVoz(String tomVoz) {
		this.tomVoz = tomVoz;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
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

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}
}