package com.assistente.empresarial.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SaasOverviewResponseDTO {
    // Metadados do filtro aplicado
    private String dataInicio;
    private String dataFim;
    private int dias;
    private UUID empresaFiltroId;
    private String empresaFiltroNome;

    // Métricas Gerais de Clientes e Planos
    private long totalEmpresas;
    private long empresasAtivas;
    private long empresasInativas;
    private long empresasPlanoSimples;
    private long empresasPlanoEmpresarial;
    private long empresasComCreditoBaixo;

    // Métricas Gerais de Créditos e Tokens
    private long totalCreditosDisponiveis;
    private long totalTokensConsumidosHistorico;
    private long totalCreditosInseridosAdmin;
    private long totalCreditosRemovidosAdmin;
    private long totalRecargasClientes;
    private long creditosInseridosNoPeriodo;

    // Métricas de Uso & Desempenho no Período
    private long tokensConsumidosNoPeriodo;
    private long tokensInputNoPeriodo;
    private long tokensOutputNoPeriodo;
    private long requisicoesNoPeriodo;
    private double custoEstimadoPeriodoUsd;
    private double latenciaMediaPeriodoMs;
    private double taxaCacheHitPeriodoPercent;
    private double taxaFallbackPeriodoPercent;

    // Métricas Gerais de Infraestrutura & Cotas
    private long totalConversasPlataforma;
    private long conversasNoPeriodo;
    private long totalDocumentosPlataforma;
    private long armazenamentoTotalBytes;
    private long totalAssistentesPlataforma;
    private long totalUsuariosPlataforma;

    // Séries e Tabelas
    private List<UsoPeriodoPontoDTO> serieDiaria = new ArrayList<>();
    private List<UsoClientePeriodoDTO> usoPorCliente = new ArrayList<>();
    private Map<String, Long> distribuicaoProvedores = new LinkedHashMap<>();
    private Map<String, Long> distribuicaoCanais = new LinkedHashMap<>();
    private List<ClienteCotaDTO> clientes = new ArrayList<>();
    private List<CreditoTransacaoDTO> transacoesRecentes = new ArrayList<>();

    public String getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(String dataInicio) {
        this.dataInicio = dataInicio;
    }

    public String getDataFim() {
        return dataFim;
    }

    public void setDataFim(String dataFim) {
        this.dataFim = dataFim;
    }

    public int getDias() {
        return dias;
    }

    public void setDias(int dias) {
        this.dias = dias;
    }

    public UUID getEmpresaFiltroId() {
        return empresaFiltroId;
    }

    public void setEmpresaFiltroId(UUID empresaFiltroId) {
        this.empresaFiltroId = empresaFiltroId;
    }

    public String getEmpresaFiltroNome() {
        return empresaFiltroNome;
    }

    public void setEmpresaFiltroNome(String empresaFiltroNome) {
        this.empresaFiltroNome = empresaFiltroNome;
    }

    public long getTotalEmpresas() {
        return totalEmpresas;
    }

    public void setTotalEmpresas(long totalEmpresas) {
        this.totalEmpresas = totalEmpresas;
    }

    public long getEmpresasAtivas() {
        return empresasAtivas;
    }

    public void setEmpresasAtivas(long empresasAtivas) {
        this.empresasAtivas = empresasAtivas;
    }

    public long getEmpresasInativas() {
        return empresasInativas;
    }

    public void setEmpresasInativas(long empresasInativas) {
        this.empresasInativas = empresasInativas;
    }

    public long getEmpresasPlanoSimples() {
        return empresasPlanoSimples;
    }

    public void setEmpresasPlanoSimples(long empresasPlanoSimples) {
        this.empresasPlanoSimples = empresasPlanoSimples;
    }

    public long getEmpresasPlanoEmpresarial() {
        return empresasPlanoEmpresarial;
    }

    public void setEmpresasPlanoEmpresarial(long empresasPlanoEmpresarial) {
        this.empresasPlanoEmpresarial = empresasPlanoEmpresarial;
    }

    public long getEmpresasComCreditoBaixo() {
        return empresasComCreditoBaixo;
    }

    public void setEmpresasComCreditoBaixo(long empresasComCreditoBaixo) {
        this.empresasComCreditoBaixo = empresasComCreditoBaixo;
    }

    public long getTotalCreditosDisponiveis() {
        return totalCreditosDisponiveis;
    }

    public void setTotalCreditosDisponiveis(long totalCreditosDisponiveis) {
        this.totalCreditosDisponiveis = totalCreditosDisponiveis;
    }

    public long getTotalTokensConsumidosHistorico() {
        return totalTokensConsumidosHistorico;
    }

    public void setTotalTokensConsumidosHistorico(long totalTokensConsumidosHistorico) {
        this.totalTokensConsumidosHistorico = totalTokensConsumidosHistorico;
    }

    public long getTotalCreditosInseridosAdmin() {
        return totalCreditosInseridosAdmin;
    }

    public void setTotalCreditosInseridosAdmin(long totalCreditosInseridosAdmin) {
        this.totalCreditosInseridosAdmin = totalCreditosInseridosAdmin;
    }

    public long getTotalCreditosRemovidosAdmin() {
        return totalCreditosRemovidosAdmin;
    }

    public void setTotalCreditosRemovidosAdmin(long totalCreditosRemovidosAdmin) {
        this.totalCreditosRemovidosAdmin = totalCreditosRemovidosAdmin;
    }

    public long getTotalRecargasClientes() {
        return totalRecargasClientes;
    }

    public void setTotalRecargasClientes(long totalRecargasClientes) {
        this.totalRecargasClientes = totalRecargasClientes;
    }

    public long getCreditosInseridosNoPeriodo() {
        return creditosInseridosNoPeriodo;
    }

    public void setCreditosInseridosNoPeriodo(long creditosInseridosNoPeriodo) {
        this.creditosInseridosNoPeriodo = creditosInseridosNoPeriodo;
    }

    public long getTokensConsumidosNoPeriodo() {
        return tokensConsumidosNoPeriodo;
    }

    public void setTokensConsumidosNoPeriodo(long tokensConsumidosNoPeriodo) {
        this.tokensConsumidosNoPeriodo = tokensConsumidosNoPeriodo;
    }

    public long getTokensInputNoPeriodo() {
        return tokensInputNoPeriodo;
    }

    public void setTokensInputNoPeriodo(long tokensInputNoPeriodo) {
        this.tokensInputNoPeriodo = tokensInputNoPeriodo;
    }

    public long getTokensOutputNoPeriodo() {
        return tokensOutputNoPeriodo;
    }

    public void setTokensOutputNoPeriodo(long tokensOutputNoPeriodo) {
        this.tokensOutputNoPeriodo = tokensOutputNoPeriodo;
    }

    public long getRequisicoesNoPeriodo() {
        return requisicoesNoPeriodo;
    }

    public void setRequisicoesNoPeriodo(long requisicoesNoPeriodo) {
        this.requisicoesNoPeriodo = requisicoesNoPeriodo;
    }

    public double getCustoEstimadoPeriodoUsd() {
        return custoEstimadoPeriodoUsd;
    }

    public void setCustoEstimadoPeriodoUsd(double custoEstimadoPeriodoUsd) {
        this.custoEstimadoPeriodoUsd = custoEstimadoPeriodoUsd;
    }

    public double getLatenciaMediaPeriodoMs() {
        return latenciaMediaPeriodoMs;
    }

    public void setLatenciaMediaPeriodoMs(double latenciaMediaPeriodoMs) {
        this.latenciaMediaPeriodoMs = latenciaMediaPeriodoMs;
    }

    public double getTaxaCacheHitPeriodoPercent() {
        return taxaCacheHitPeriodoPercent;
    }

    public void setTaxaCacheHitPeriodoPercent(double taxaCacheHitPeriodoPercent) {
        this.taxaCacheHitPeriodoPercent = taxaCacheHitPeriodoPercent;
    }

    public double getTaxaFallbackPeriodoPercent() {
        return taxaFallbackPeriodoPercent;
    }

    public void setTaxaFallbackPeriodoPercent(double taxaFallbackPeriodoPercent) {
        this.taxaFallbackPeriodoPercent = taxaFallbackPeriodoPercent;
    }

    public long getTotalConversasPlataforma() {
        return totalConversasPlataforma;
    }

    public void setTotalConversasPlataforma(long totalConversasPlataforma) {
        this.totalConversasPlataforma = totalConversasPlataforma;
    }

    public long getConversasNoPeriodo() {
        return conversasNoPeriodo;
    }

    public void setConversasNoPeriodo(long conversasNoPeriodo) {
        this.conversasNoPeriodo = conversasNoPeriodo;
    }

    public long getTotalDocumentosPlataforma() {
        return totalDocumentosPlataforma;
    }

    public void setTotalDocumentosPlataforma(long totalDocumentosPlataforma) {
        this.totalDocumentosPlataforma = totalDocumentosPlataforma;
    }

    public long getArmazenamentoTotalBytes() {
        return armazenamentoTotalBytes;
    }

    public void setArmazenamentoTotalBytes(long armazenamentoTotalBytes) {
        this.armazenamentoTotalBytes = armazenamentoTotalBytes;
    }

    public long getTotalAssistentesPlataforma() {
        return totalAssistentesPlataforma;
    }

    public void setTotalAssistentesPlataforma(long totalAssistentesPlataforma) {
        this.totalAssistentesPlataforma = totalAssistentesPlataforma;
    }

    public long getTotalUsuariosPlataforma() {
        return totalUsuariosPlataforma;
    }

    public void setTotalUsuariosPlataforma(long totalUsuariosPlataforma) {
        this.totalUsuariosPlataforma = totalUsuariosPlataforma;
    }

    public List<UsoPeriodoPontoDTO> getSerieDiaria() {
        return serieDiaria;
    }

    public void setSerieDiaria(List<UsoPeriodoPontoDTO> serieDiaria) {
        this.serieDiaria = serieDiaria;
    }

    public List<UsoClientePeriodoDTO> getUsoPorCliente() {
        return usoPorCliente;
    }

    public void setUsoPorCliente(List<UsoClientePeriodoDTO> usoPorCliente) {
        this.usoPorCliente = usoPorCliente;
    }

    public Map<String, Long> getDistribuicaoProvedores() {
        return distribuicaoProvedores;
    }

    public void setDistribuicaoProvedores(Map<String, Long> distribuicaoProvedores) {
        this.distribuicaoProvedores = distribuicaoProvedores;
    }

    public Map<String, Long> getDistribuicaoCanais() {
        return distribuicaoCanais;
    }

    public void setDistribuicaoCanais(Map<String, Long> distribuicaoCanais) {
        this.distribuicaoCanais = distribuicaoCanais;
    }

    public List<ClienteCotaDTO> getClientes() {
        return clientes;
    }

    public void setClientes(List<ClienteCotaDTO> clientes) {
        this.clientes = clientes;
    }

    public List<CreditoTransacaoDTO> getTransacoesRecentes() {
        return transacoesRecentes;
    }

    public void setTransacoesRecentes(List<CreditoTransacaoDTO> transacoesRecentes) {
        this.transacoesRecentes = transacoesRecentes;
    }
}
