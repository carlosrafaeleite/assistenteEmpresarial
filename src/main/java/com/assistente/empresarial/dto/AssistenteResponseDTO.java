package com.assistente.empresarial.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class AssistenteResponseDTO {
	private UUID id;
	private String nome;
	private String descricao;
	private String promptSistema;
	private String corPrimaria;
	private String corSecundaria;
	private String avatarUrl;
	private String mensagemBoasVindas;
	private String tomVoz;
	private boolean ativo;
	private LocalDateTime createdAt;

	public AssistenteResponseDTO() {}

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
}