package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.FuncionarioRequestDTO;
import com.assistente.empresarial.exception.ResourceNotFoundException;
import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.model.Funcionario;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.repository.FuncionarioRepository;
import com.assistente.empresarial.security.TenantContext;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID; // Altere para Long se o ID da empresa for do tipo Long

@Service
public class FuncionarioService {

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Empresa getEmpresaAtual() {
        UUID tenantId = TenantContext.getTenantId();
        return empresaRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa do tenant atual não encontrada."));
    }

    public List<Funcionario> listarDaEmpresa() {
        Empresa empresaAtual = getEmpresaAtual();
        return funcionarioRepository.findByEmpresa(empresaAtual);
    }

    public Funcionario buscarPorId(Long id) {
        Empresa empresaAtual = getEmpresaAtual();
        return funcionarioRepository.findById(id)
                .filter(f -> f.getEmpresa().getId().equals(empresaAtual.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado ou não pertence a esta empresa."));
    }

    @Transactional
    public Funcionario criar(FuncionarioRequestDTO dto) {
        Empresa empresaAtual = getEmpresaAtual();

        Funcionario funcionario = modelMapper.map(dto, Funcionario.class);
        funcionario.setSenha(passwordEncoder.encode(dto.getSenha()));
        funcionario.setEmpresa(empresaAtual);
        funcionario.setAtivo(true);

        return funcionarioRepository.save(funcionario);
    }

    @Transactional
    public Funcionario atualizar(Long id, FuncionarioRequestDTO dto) {
        Funcionario funcionario = buscarPorId(id); // Garante que pertence à empresa logada

        funcionario.setNome(dto.getNome());
        funcionario.setEmail(dto.getEmail());

        // Só atualiza a senha se vier preenchida no DTO
        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            funcionario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        return funcionarioRepository.save(funcionario);
    }

    @Transactional
    public void excluir(Long id) {
        Funcionario funcionario = buscarPorId(id); // Garante que pertence à empresa logada
        funcionarioRepository.delete(funcionario);
    }
}