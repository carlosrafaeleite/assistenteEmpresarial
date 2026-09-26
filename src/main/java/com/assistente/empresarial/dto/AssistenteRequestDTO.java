package com.assistente.empresarial.dto;

public class AssistenteRequestDTO {
	private String nome;
	private String descricao;
	private String promptSistema;
	private boolean ativo;

	// Getters e Setters (omitidos para brevidade, mas devem ser gerados)
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
}