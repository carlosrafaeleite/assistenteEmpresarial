package com.assistente.empresarial.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class UsuarioResponseDTO {
	private UUID id;
	private UUID empresaId;
	private String nome;
	private String email;
	private String perfil;
	private boolean status;
	private LocalDateTime createdAt;

	// Getters e Setters necessários para o ModelMapper
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}