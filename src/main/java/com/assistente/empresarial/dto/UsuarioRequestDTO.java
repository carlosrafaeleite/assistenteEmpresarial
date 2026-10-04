package com.assistente.empresarial.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UsuarioRequestDTO {

	@NotBlank(message = "O nome é obrigatório.")
	private String nome;

	@NotBlank(message = "O e-mail é obrigatório.")
	@Email(message = "Formato de e-mail inválido.")
	private String email;

	private String senha;

	@NotBlank(message = "O perfil é obrigatório.")
	private String perfil; // ADMIN ou FUNCIONARIO

	private boolean status = true;

	public UsuarioRequestDTO() {}

	// Getters e Setters
	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public String getPerfil() {
		return perfil;
	}

	public void setPerfil(String perfil) {
		this.perfil = perfil;
	}

	public boolean isStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}
}