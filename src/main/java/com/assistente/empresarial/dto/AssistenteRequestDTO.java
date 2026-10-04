package com.assistente.empresarial.dto;

import jakarta.validation.constraints.NotBlank;

public class AssistenteRequestDTO {

	@NotBlank(message = "O nome do assistente é obrigatório.")
	private String nome;

	private String descricao;

	private String promptSistema;

	private String corPrimaria = "#2563EB";

	private String corSecundaria = "#1E40AF";

	private String avatarUrl;

	private String mensagemBoasVindas = "Olá! Como posso ajudar você hoje?";

	private String tomVoz = "AMIGAVEL";

	private boolean ativo = true;

	public AssistenteRequestDTO() {}

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
}