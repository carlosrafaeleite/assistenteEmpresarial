package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.FuncionarioRequestDTO;
import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.model.Funcionario;
import com.assistente.empresarial.repository.EmpresaRepository;
import com.assistente.empresarial.repository.FuncionarioRepository;
import com.assistente.empresarial.security.TenantContext;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID; // Altere para Long se o ID da empresa for do tipo Long

@Service
public class FuncionarioService {

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private ModelMapper modelMapper; // Injeta o ModelMapper configurado no teu AppConfig

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Método auxiliar para buscar a empresa atual com base no TenantContext
    private Empresa getEmpresaAtual() {
        UUID tenantId = TenantContext.getTenantId(); // Se o ID for Long, ajuste para Long
        return empresaRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Empresa do tenant atual não encontrada."));
    }

    public List<Funcionario> listarDaEmpresa() {
        Empresa empresaAtual = getEmpresaAtual();
        return funcionarioRepository.findByEmpresa(empresaAtual);
    }

    public Funcionario criar(FuncionarioRequestDTO dto) {
        Empresa empresaAtual = getEmpresaAtual();

        Funcionario funcionario = modelMapper.map(dto, Funcionario.class);

        // Atribuições específicas que exigem lógica de negócio
        funcionario.setSenha(passwordEncoder.encode(dto.getSenha()));
        funcionario.setEmpresa(empresaAtual);
        funcionario.setAtivo(true);

        return funcionarioRepository.save(funcionario);
    }
}