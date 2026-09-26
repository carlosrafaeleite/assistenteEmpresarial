package com.assistente.empresarial.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class AssistenteResponseDTO {
	private UUID id;
	private String nome;
	private String descricao;
	private String promptSistema;
	private boolean ativo;
	private LocalDateTime createdAt;

	// Getters e Setters (omitidos para brevidade, mas devem ser gerados)
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