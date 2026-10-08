package com.assistente.empresarial.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmpresaRegistroRequestDTO {

	// Dados da Empresa
	@NotBlank(message = "O nome da empresa é obrigatório.")
	private String nomeEmpresa;

	private String razaoSocial;


	@NotBlank(message = "O slug é obrigatório.")
	@Pattern(regexp = "^[a-z0-9-]+$", message = "O slug deve conter apenas letras minúsculas, números e hífens.")
	private String slug;

	// Dados do Utilizador Admin
	@NotBlank(message = "O nome do administrador é obrigatório.")
	private String nomeAdmin;

	@NotBlank(message = "O e-mail do administrador é obrigatório.")
	@Email(message = "Formato de e-mail inválido.")
	private String emailAdmin;

	@NotBlank(message = "A senha do administrador é obrigatória.")
	@Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres.")
	private String senhaAdmin;

	public EmpresaRegistroRequestDTO() {}

	// Getters e Setters
	public String getNomeEmpresa() {
		return nomeEmpresa;
	}

	public void setNomeEmpresa(String nomeEmpresa) {
		this.nomeEmpresa = nomeEmpresa;
	}

	public String getRazaoSocial() {
		return razaoSocial;
	}

	public void setRazaoSocial(String razaoSocial) {
		this.razaoSocial = razaoSocial;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getNomeAdmin() {
		return nomeAdmin;
	}

	public void setNomeAdmin(String nomeAdmin) {
		this.nomeAdmin = nomeAdmin;
	}

	public String getEmailAdmin() {
		return emailAdmin;
	}

	public void setEmailAdmin(String emailAdmin) {
		this.emailAdmin = emailAdmin;
	}

	public String getSenhaAdmin() {
		return senhaAdmin;
	}

	public void setSenhaAdmin(String senhaAdmin) {
		this.senhaAdmin = senhaAdmin;
	}
}