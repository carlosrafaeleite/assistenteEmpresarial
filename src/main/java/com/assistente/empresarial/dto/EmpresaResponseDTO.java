package com.assistente.empresarial.dto;

import jakarta.persistence.Column;

import java.time.LocalDateTime;
import java.util.UUID;

public class EmpresaResponseDTO {
	private UUID id;
	private String nome;
	private String razaoSocial;
	private String slug;
	private String email;
	private boolean status;
	private LocalDateTime createdAt;
	private String nomeBot;

	// Getters e Setters
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

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	public String getNomeBot() {
		return nomeBot;
	}

	public void setNomeBot(String nomeBot) {
		this.nomeBot = nomeBot;
	}
}