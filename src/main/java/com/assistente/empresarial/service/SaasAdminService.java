package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.*;
import com.assistente.empresarial.enuns.PlanoEmpresa;
import com.assistente.empresarial.enuns.TipoTransacaoCredito;
import com.assistente.empresarial.exception.BusinessException;
import com.assistente.empresarial.exception.ResourceNotFoundException;
import com.assistente.empresarial.model.ChatTrace;
import com.assistente.empresarial.model.Conversa;
import com.assistente.empresarial.model.CreditoTransacao;
import com.assistente.empresarial.model.Empresa;
import com.assistente.empresarial.model.Usuario;
import com.assistente.empresarial.repository.*;
import com.assistente.empresarial.security.SaasOwnerAuthorizationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SaasAdminService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter LABEL_DD_MM = DateTimeFormatter.ofPattern("dd/MM");
    private static final long LIMIAR_CREDITO_BAIXO_TOKENS = 10_000L;
    private static final double LIMIAR_CREDITO_BAIXO_PERCENT = 85.0;

    private final SaasOwnerAuthorizationService ownerAuthService;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AssistenteRepository assistenteRepository;
    private final DocumentoRepository documentoRepository;
    private final ConversaRepository conversaRepository;
    private final ChatTraceJpaRepository chatTraceJpaRepository;
    private final CreditoTransacaoRepository creditoTransacaoRepository;

    public SaasAdminService(SaasOwnerAuthorizationService ownerAuthService,
                            EmpresaRepository empresaRepository,
                            UsuarioRepository usuarioRepository,
                            AssistenteRepository assistenteRepository,
                            DocumentoRepository documentoRepository,
                            ConversaRepository conversaRepository,
                            ChatTraceJpaRepository chatTraceJpaRepository,
                            CreditoTransacaoRepository creditoTransacaoRepository) {
        this.ownerAuthService = ownerAuthService;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.assistenteRepository = assistenteRepository;
        this.documentoRepository = documentoRepository;
        this.conversaRepository = conversaRepository;
        this.chatTraceJpaRepository = chatTraceJpaRepository;
        this.creditoTransacaoRepository = creditoTransacaoRepository;
    }

    public SaasOverviewResponseDTO obterVisaoGeral(Integer diasParam,
                                                   LocalDate dataInicioParam,
                                                   LocalDate dataFimParam,
                                                   UUID empresaFiltroId) {
        ownerAuthService.assertCurrentUserIsSaasOwner();

        LocalDate dataFim = dataFimParam != null ? dataFimParam : LocalDate.now();
        int dias = (diasParam != null && diasParam > 0) ? Math.min(diasParam, 365) : 30;
        LocalDate dataInicio = dataInicioParam != null ? dataInicioParam : dataFim.minusDays(dias - 1L);

        if (dataInicio.isAfter(dataFim)) {
            LocalDate tmp = dataInicio;
            dataInicio = dataFim;
            dataFim = tmp;
        }

        long diasCalculados = ChronoUnit.DAYS.between(dataInicio, dataFim) + 1;
        if (diasCalculados > 365) {
            dataInicio = dataFim.minusDays(364);
            diasCalculados = 365;
        }

        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(LocalTime.MAX);

        List<Empresa> todasEmpresas = empresaRepository.findAll();
        todasEmpresas.sort(Comparator.comparing(Empresa::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        Map<UUID, Empresa> empresaMap = new HashMap<>();
        for (Empresa e : todasEmpresas) {
            empresaMap.put(e.getId(), e);
        }

        String empresaFiltroNome = null;
        if (empresaFiltroId != null) {
            Empresa filtrada = empresaMap.get(empresaFiltroId);
            if (filtrada == null) {
                throw new ResourceNotFoundException("Empresa filtrada não encontrada.");
            }
            empresaFiltroNome = filtrada.getNome();
        }

        // Busca traces e conversas do período (todos para ranking por cliente + filtrados para série temporal)
        List<ChatTrace> tracesTodosPeriodo = chatTraceJpaRepository
                .findByTimestampBetweenOrderByTimestampAsc(inicioDateTime, fimDateTime);
        List<ChatTrace> tracesFiltrados = (empresaFiltroId == null)
                ? tracesTodosPeriodo
                : tracesTodosPeriodo.stream()
                .filter(t -> empresaFiltroId.equals(t.getEmpresaId()))
                .collect(Collectors.toList());

        List<Conversa> conversasTodosPeriodo = conversaRepository
                .findByCreatedAtBetween(inicioDateTime, fimDateTime);
        List<Conversa> conversasFiltradas = (empresaFiltroId == null)
                ? conversasTodosPeriodo
                : conversasTodosPeriodo.stream()
                .filter(c -> empresaFiltroId.equals(c.getEmpresaId()))
                .collect(Collectors.toList());

        List<CreditoTransacao> todasTransacoes = creditoTransacaoRepository.findAll();
        List<CreditoTransacao> transacoesPeriodoTodas = creditoTransacaoRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(inicioDateTime, fimDateTime);
        List<CreditoTransacao> transacoesPeriodoFiltradas = (empresaFiltroId == null)
                ? transacoesPeriodoTodas
                : transacoesPeriodoTodas.stream()
                .filter(t -> empresaFiltroId.equals(t.getEmpresaId()))
                .collect(Collectors.toList());

        // Mapas auxiliares por empresa
        Map<UUID, Long> creditosInseridosAdminPorEmpresa = new HashMap<>();
        long totalCreditosInseridosAdmin = 0L;
        long totalCreditosRemovidosAdmin = 0L;
        long totalRecargasClientes = 0L;

        for (CreditoTransacao tx : todasTransacoes) {
            if (tx.getTipo() == TipoTransacaoCredito.INSERCAO_ADMIN
                    || (tx.getTipo() == TipoTransacaoCredito.AJUSTE_ADMIN && tx.getQuantidadeTokens() > 0)) {
                totalCreditosInseridosAdmin += Math.max(0L, tx.getQuantidadeTokens());
                creditosInseridosAdminPorEmpresa.merge(tx.getEmpresaId(), Math.max(0L, tx.getQuantidadeTokens()), Long::sum);
            } else if (tx.getTipo() == TipoTransacaoCredito.REMOCAO_ADMIN
                    || (tx.getTipo() == TipoTransacaoCredito.AJUSTE_ADMIN && tx.getQuantidadeTokens() < 0)) {
                totalCreditosRemovidosAdmin += Math.abs(tx.getQuantidadeTokens());
            } else if (tx.getTipo() == TipoTransacaoCredito.RECARGA_CLIENTE) {
                totalRecargasClientes += Math.max(0L, tx.getQuantidadeTokens());
            }
        }

        // Agrega traces por empresa no período
        Map<UUID, List<ChatTrace>> tracesPorEmpresa = new HashMap<>();
        for (ChatTrace trace : tracesTodosPeriodo) {
            if (trace.getEmpresaId() != null) {
                tracesPorEmpresa.computeIfAbsent(trace.getEmpresaId(), k -> new ArrayList<>()).add(trace);
            }
        }

        Map<UUID, Long> conversasPeriodoPorEmpresa = new HashMap<>();
        for (Conversa conv : conversasTodosPeriodo) {
            if (conv.getEmpresaId() != null) {
                conversasPeriodoPorEmpresa.merge(conv.getEmpresaId(), 1L, Long::sum);
            }
        }

        Map<UUID, Long> creditosInseridosPeriodoPorEmpresa = new HashMap<>();
        for (CreditoTransacao tx : transacoesPeriodoTodas) {
            if (tx.getQuantidadeTokens() > 0) {
                creditosInseridosPeriodoPorEmpresa.merge(tx.getEmpresaId(), tx.getQuantidadeTokens(), Long::sum);
            }
        }

        // Constrói DTOs de clientes com cotas + KPIs globais
        SaasOverviewResponseDTO overview = new SaasOverviewResponseDTO();
        overview.setDataInicio(dataInicio.format(ISO_DATE));
        overview.setDataFim(dataFim.format(ISO_DATE));
        overview.setDias((int) diasCalculados);
        overview.setEmpresaFiltroId(empresaFiltroId);
        overview.setEmpresaFiltroNome(empresaFiltroNome);

        long empresasAtivas = 0;
        long empresasInativas = 0;
        long planoSimples = 0;
        long planoEmpresarial = 0;
        long creditosBaixos = 0;
        long totalCreditosDisponiveis = 0;
        long totalTokensConsumidosHistorico = 0;
        long totalConversasPlataforma = 0;
        long totalDocumentosPlataforma = 0;
        long armazenamentoTotalBytes = 0;
        long totalAssistentesPlataforma = 0;
        long totalUsuariosPlataforma = 0;

        List<ClienteCotaDTO> clientesDto = new ArrayList<>();
        List<UsoClientePeriodoDTO> usoPorClienteList = new ArrayList<>();

        for (Empresa empresa : todasEmpresas) {
            UUID empId = empresa.getId();
            if (empresa.isStatus()) empresasAtivas++; else empresasInativas++;
            if (empresa.getPlano() == PlanoEmpresa.EMPRESARIAL) planoEmpresarial++; else planoSimples++;

            long docsUsados = documentoRepository.countByEmpresaId(empId);
            long bytesUsados = documentoRepository.sumTamanhoBytesByEmpresaId(empId);
            long assistentesUsados = assistenteRepository.countByEmpresaId(empId);
            long usuariosTotal = usuarioRepository.countByEmpresaId(empId);
            long conversasTotal = conversaRepository.countByEmpresaId(empId);

            List<ChatTrace> tracesEmp = tracesPorEmpresa.getOrDefault(empId, Collections.emptyList());
            long tokensPeriodoEmp = 0L;
            double custoPeriodoEmp = 0.0;
            long somaLatenciaEmp = 0L;
            LocalDateTime ultimaAtividade = empresa.getUpdatedAt();
            for (ChatTrace t : tracesEmp) {
                tokensPeriodoEmp += t.getTotalTokens();
                custoPeriodoEmp += t.getEstimatedCostUsd();
                somaLatenciaEmp += t.getTotalLatencyMs();
                if (t.getTimestamp() != null && (ultimaAtividade == null || t.getTimestamp().isAfter(ultimaAtividade))) {
                    ultimaAtividade = t.getTimestamp();
                }
            }

            ClienteCotaDTO clienteDto = montarClienteCotaDTO(
                    empresa,
                    docsUsados,
                    bytesUsados,
                    assistentesUsados,
                    usuariosTotal,
                    conversasTotal,
                    creditosInseridosAdminPorEmpresa.getOrDefault(empId, 0L),
                    tracesEmp.size(),
                    tokensPeriodoEmp,
                    custoPeriodoEmp,
                    ultimaAtividade
            );
            clientesDto.add(clienteDto);

            if (clienteDto.isAlertaCreditoBaixo() && empresa.isStatus()) {
                creditosBaixos++;
            }

            totalCreditosDisponiveis += empresa.getCreditosTokens();
            totalTokensConsumidosHistorico += empresa.getTokensConsumidos();
            totalConversasPlataforma += conversasTotal;
            totalDocumentosPlataforma += docsUsados;
            armazenamentoTotalBytes += bytesUsados;
            totalAssistentesPlataforma += assistentesUsados;
            totalUsuariosPlataforma += usuariosTotal;

            UsoClientePeriodoDTO usoCliente = new UsoClientePeriodoDTO();
            usoCliente.setEmpresaId(empId);
            usoCliente.setEmpresaNome(empresa.getNome());
            usoCliente.setPlano(empresa.getPlano());
            usoCliente.setStatus(empresa.isStatus());
            usoCliente.setTokensConsumidosPeriodo(tokensPeriodoEmp);
            usoCliente.setRequisicoesPeriodo(tracesEmp.size());
            usoCliente.setCustoEstimadoUsd(custoPeriodoEmp);
            usoCliente.setLatenciaMediaMs(tracesEmp.isEmpty() ? 0.0 : (double) somaLatenciaEmp / tracesEmp.size());
            usoCliente.setConversasPeriodo(conversasPeriodoPorEmpresa.getOrDefault(empId, 0L));
            usoCliente.setCreditosInseridosPeriodo(creditosInseridosPeriodoPorEmpresa.getOrDefault(empId, 0L));
            usoCliente.setCreditosRestantes(empresa.getCreditosTokens());
            usoCliente.setTokensConsumidosTotal(empresa.getTokensConsumidos());
            usoPorClienteList.add(usoCliente);
        }

        usoPorClienteList.sort(
                Comparator.comparingLong(UsoClientePeriodoDTO::getTokensConsumidosPeriodo).reversed()
                        .thenComparingLong(UsoClientePeriodoDTO::getTokensConsumidosTotal).reversed()
        );

        // Série diária contínua de dataInicio até dataFim
        Map<LocalDate, UsoPeriodoPontoDTO> pontosPorDia = new LinkedHashMap<>();
        Map<LocalDate, Long> somaLatenciaPorDia = new HashMap<>();
        for (LocalDate d = dataInicio; !d.isAfter(dataFim); d = d.plusDays(1)) {
            UsoPeriodoPontoDTO ponto = new UsoPeriodoPontoDTO();
            ponto.setData(d.format(ISO_DATE));
            ponto.setLabel(d.format(LABEL_DD_MM));
            pontosPorDia.put(d, ponto);
            somaLatenciaPorDia.put(d, 0L);
        }

        long tokensPeriodoTotal = 0L;
        long tokensInputPeriodo = 0L;
        long tokensOutputPeriodo = 0L;
        long cacheHitsPeriodo = 0L;
        long fallbacksPeriodo = 0L;
        long somaLatenciaPeriodo = 0L;
        double custoPeriodoTotal = 0.0;
        Map<String, Long> distProvedores = new LinkedHashMap<>();
        Map<String, Long> distCanais = new LinkedHashMap<>();

        for (ChatTrace t : tracesFiltrados) {
            tokensPeriodoTotal += t.getTotalTokens();
            tokensInputPeriodo += t.getInputTokens();
            tokensOutputPeriodo += t.getOutputTokens();
            custoPeriodoTotal += t.getEstimatedCostUsd();
            somaLatenciaPeriodo += t.getTotalLatencyMs();
            if (t.isCacheHit()) cacheHitsPeriodo++;
            if (t.isFallbackTriggered()) fallbacksPeriodo++;

            String prov = t.getProviderUsed() != null ? t.getProviderUsed().name() : "GEMINI";
            distProvedores.merge(prov, 1L, Long::sum);

            String canal = (t.getCanal() != null && !t.getCanal().isBlank()) ? t.getCanal() : "WIDGET";
            distCanais.merge(canal, 1L, Long::sum);

            if (t.getTimestamp() != null) {
                LocalDate dia = t.getTimestamp().toLocalDate();
                UsoPeriodoPontoDTO ponto = pontosPorDia.get(dia);
                if (ponto != null) {
                    ponto.setTokensInput(ponto.getTokensInput() + t.getInputTokens());
                    ponto.setTokensOutput(ponto.getTokensOutput() + t.getOutputTokens());
                    ponto.setTokensTotal(ponto.getTokensTotal() + t.getTotalTokens());
                    ponto.setRequisicoes(ponto.getRequisicoes() + 1);
                    if (t.isCacheHit()) ponto.setCacheHits(ponto.getCacheHits() + 1);
                    if (t.isFallbackTriggered()) ponto.setFallbacks(ponto.getFallbacks() + 1);
                    ponto.setCustoEstimadoUsd(ponto.getCustoEstimadoUsd() + t.getEstimatedCostUsd());
                    somaLatenciaPorDia.merge(dia, t.getTotalLatencyMs(), Long::sum);
                }
            }
        }

        for (Conversa conv : conversasFiltradas) {
            if (conv.getCreatedAt() != null) {
                UsoPeriodoPontoDTO ponto = pontosPorDia.get(conv.getCreatedAt().toLocalDate());
                if (ponto != null) {
                    ponto.setConversasCriadas(ponto.getConversasCriadas() + 1);
                }
            }
        }

        long creditosInseridosPeriodo = 0L;
        for (CreditoTransacao tx : transacoesPeriodoFiltradas) {
            if (tx.getCreatedAt() != null) {
                UsoPeriodoPontoDTO ponto = pontosPorDia.get(tx.getCreatedAt().toLocalDate());
                if (ponto != null) {
                    if (tx.getQuantidadeTokens() > 0) {
                        ponto.setCreditosInseridos(ponto.getCreditosInseridos() + tx.getQuantidadeTokens());
                    } else if (tx.getQuantidadeTokens() < 0) {
                        ponto.setCreditosRemovidos(ponto.getCreditosRemovidos() + Math.abs(tx.getQuantidadeTokens()));
                    }
                }
            }
            if (tx.getQuantidadeTokens() > 0
                    && (tx.getTipo() == TipoTransacaoCredito.INSERCAO_ADMIN
                    || tx.getTipo() == TipoTransacaoCredito.AJUSTE_ADMIN
                    || tx.getTipo() == TipoTransacaoCredito.RECARGA_CLIENTE)) {
                creditosInseridosPeriodo += tx.getQuantidadeTokens();
            }
        }

        for (Map.Entry<LocalDate, UsoPeriodoPontoDTO> entry : pontosPorDia.entrySet()) {
            UsoPeriodoPontoDTO ponto = entry.getValue();
            if (ponto.getRequisicoes() > 0) {
                ponto.setLatenciaMediaMs((double) somaLatenciaPorDia.getOrDefault(entry.getKey(), 0L) / ponto.getRequisicoes());
            }
        }

        long reqPeriodo = tracesFiltrados.size();
        overview.setTotalEmpresas(todasEmpresas.size());
        overview.setEmpresasAtivas(empresasAtivas);
        overview.setEmpresasInativas(empresasInativas);
        overview.setEmpresasPlanoSimples(planoSimples);
        overview.setEmpresasPlanoEmpresarial(planoEmpresarial);
        overview.setEmpresasComCreditoBaixo(creditosBaixos);

        overview.setTotalCreditosDisponiveis(totalCreditosDisponiveis);
        overview.setTotalTokensConsumidosHistorico(totalTokensConsumidosHistorico);
        overview.setTotalCreditosInseridosAdmin(totalCreditosInseridosAdmin);
        overview.setTotalCreditosRemovidosAdmin(totalCreditosRemovidosAdmin);
        overview.setTotalRecargasClientes(totalRecargasClientes);
        overview.setCreditosInseridosNoPeriodo(creditosInseridosPeriodo);

        overview.setTokensConsumidosNoPeriodo(tokensPeriodoTotal);
        overview.setTokensInputNoPeriodo(tokensInputPeriodo);
        overview.setTokensOutputNoPeriodo(tokensOutputPeriodo);
        overview.setRequisicoesNoPeriodo(reqPeriodo);
        overview.setCustoEstimadoPeriodoUsd(custoPeriodoTotal);
        overview.setLatenciaMediaPeriodoMs(reqPeriodo > 0 ? (double) somaLatenciaPeriodo / reqPeriodo : 0.0);
        overview.setTaxaCacheHitPeriodoPercent(reqPeriodo > 0 ? ((double) cacheHitsPeriodo / reqPeriodo) * 100.0 : 0.0);
        overview.setTaxaFallbackPeriodoPercent(reqPeriodo > 0 ? ((double) fallbacksPeriodo / reqPeriodo) * 100.0 : 0.0);

        overview.setTotalConversasPlataforma(totalConversasPlataforma);
        overview.setConversasNoPeriodo(conversasFiltradas.size());
        overview.setTotalDocumentosPlataforma(totalDocumentosPlataforma);
        overview.setArmazenamentoTotalBytes(armazenamentoTotalBytes);
        overview.setTotalAssistentesPlataforma(totalAssistentesPlataforma);
        overview.setTotalUsuariosPlataforma(totalUsuariosPlataforma);

        overview.setSerieDiaria(new ArrayList<>(pontosPorDia.values()));
        overview.setUsoPorCliente(usoPorClienteList);
        overview.setDistribuicaoProvedores(distProvedores);
        overview.setDistribuicaoCanais(distCanais);
        overview.setClientes(clientesDto);

        List<CreditoTransacao> recentes = (empresaFiltroId == null)
                ? creditoTransacaoRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50))
                : creditoTransacaoRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaFiltroId, PageRequest.of(0, 50));
        overview.setTransacoesRecentes(recentes.stream().map(this::toTransacaoDTO).collect(Collectors.toList()));

        return overview;
    }

    @Transactional
    public Map<String, Object> operarCreditosCliente(UUID empresaId, SaasCreditoOperacaoRequestDTO request) {
        Usuario owner = ownerAuthService.assertCurrentUserIsSaasOwner();

        if (request == null) {
            throw new BusinessException("Dados da operação de créditos não informados.");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa cliente não encontrada."));

        String operacao = request.getOperacao() != null ? request.getOperacao().trim().toUpperCase() : "ADICIONAR";
        long quantidade = request.getQuantidadeTokens();
        long saldoAnterior = empresa.getCreditosTokens();
        long saldoNovo;
        long delta;
        TipoTransacaoCredito tipo;

        switch (operacao) {
            case "ADICIONAR":
                if (quantidade <= 0) {
                    throw new BusinessException("A quantidade de créditos a adicionar deve ser maior que zero.");
                }
                saldoNovo = saldoAnterior + quantidade;
                delta = quantidade;
                tipo = TipoTransacaoCredito.INSERCAO_ADMIN;
                break;
            case "REMOVER":
                if (quantidade <= 0) {
                    throw new BusinessException("A quantidade de créditos a remover deve ser maior que zero.");
                }
                saldoNovo = Math.max(0L, saldoAnterior - quantidade);
                delta = saldoNovo - saldoAnterior; // negativo ou 0
                tipo = TipoTransacaoCredito.REMOCAO_ADMIN;
                break;
            case "DEFINIR":
                if (quantidade < 0) {
                    throw new BusinessException("O saldo definido não pode ser negativo.");
                }
                saldoNovo = quantidade;
                delta = saldoNovo - saldoAnterior;
                tipo = TipoTransacaoCredito.AJUSTE_ADMIN;
                break;
            default:
                throw new BusinessException("Operação inválida. Use ADICIONAR, REMOVER ou DEFINIR.");
        }

        empresa.setCreditosTokens(saldoNovo);
        empresa.setUpdatedAt(LocalDateTime.now());
        Empresa empresaSalva = empresaRepository.save(empresa);

        String motivo = (request.getMotivo() != null && !request.getMotivo().trim().isEmpty())
                ? request.getMotivo().trim()
                : switch (tipo) {
                    case INSERCAO_ADMIN -> "Inserção manual de créditos pelo administrador SaaS";
                    case REMOCAO_ADMIN -> "Remoção manual de créditos pelo administrador SaaS";
                    default -> "Ajuste manual de saldo pelo administrador SaaS";
                };

        CreditoTransacao transacao = new CreditoTransacao(
                empresaSalva.getId(),
                empresaSalva.getNome(),
                tipo,
                delta,
                saldoAnterior,
                saldoNovo,
                motivo,
                owner.getEmail()
        );
        CreditoTransacao transacaoSalva = creditoTransacaoRepository.save(transacao);

        ClienteCotaDTO clienteDto = construirClienteCotaIsolado(empresaSalva);
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("cliente", clienteDto);
        resultado.put("transacao", toTransacaoDTO(transacaoSalva));
        return resultado;
    }

    @Transactional
    public ClienteCotaDTO atualizarCotasCliente(UUID empresaId, SaasAtualizarCotasRequestDTO request) {
        Usuario owner = ownerAuthService.assertCurrentUserIsSaasOwner();

        if (request == null) {
            throw new BusinessException("Dados de atualização de cotas não informados.");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa cliente não encontrada."));

        long saldoAnterior = empresa.getCreditosTokens();

        if (request.getPlano() != null && !request.getPlano().trim().isEmpty()) {
            PlanoEmpresa novoPlano = PlanoEmpresa.fromString(request.getPlano());
            empresa.setPlano(novoPlano);
            if (Boolean.TRUE.equals(request.getConcederFranquiaPlano())
                    && empresa.getCreditosTokens() < novoPlano.getCreditosIniciais()) {
                empresa.setCreditosTokens(novoPlano.getCreditosIniciais());
            }
        }

        if (request.getLimiteAssistentes() != null) {
            if (request.getLimiteAssistentes() < 1 || request.getLimiteAssistentes() > 500) {
                throw new BusinessException("O limite de assistentes deve estar entre 1 e 500.");
            }
            empresa.setLimiteAssistentes(request.getLimiteAssistentes());
        }

        if (request.getStatus() != null) {
            empresa.setStatus(request.getStatus());
        }

        empresa.setUpdatedAt(LocalDateTime.now());
        Empresa salva = empresaRepository.save(empresa);

        if (salva.getCreditosTokens() != saldoAnterior) {
            String motivo = (request.getMotivo() != null && !request.getMotivo().trim().isEmpty())
                    ? request.getMotivo().trim()
                    : "Franquia concedida na alteração de plano para " + salva.getPlano().name();
            creditoTransacaoRepository.save(new CreditoTransacao(
                    salva.getId(),
                    salva.getNome(),
                    TipoTransacaoCredito.PLANO_UPGRADE,
                    salva.getCreditosTokens() - saldoAnterior,
                    saldoAnterior,
                    salva.getCreditosTokens(),
                    motivo,
                    owner.getEmail()
            ));
        }

        return construirClienteCotaIsolado(salva);
    }

    public List<CreditoTransacaoDTO> listarTransacoesCreditos(UUID empresaId,
                                                              LocalDate dataInicio,
                                                              LocalDate dataFim,
                                                              Integer limit) {
        ownerAuthService.assertCurrentUserIsSaasOwner();

        int max = (limit != null && limit > 0) ? Math.min(limit, 500) : 100;
        List<CreditoTransacao> lista;

        if (dataInicio != null && dataFim != null) {
            LocalDateTime inicioDt = dataInicio.atStartOfDay();
            LocalDateTime fimDt = dataFim.atTime(LocalTime.MAX);
            lista = (empresaId != null)
                    ? creditoTransacaoRepository.findByEmpresaIdAndCreatedAtBetweenOrderByCreatedAtDesc(empresaId, inicioDt, fimDt)
                    : creditoTransacaoRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(inicioDt, fimDt);
        } else {
            lista = (empresaId != null)
                    ? creditoTransacaoRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId, PageRequest.of(0, max))
                    : creditoTransacaoRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, max));
        }

        return lista.stream()
                .limit(max)
                .map(this::toTransacaoDTO)
                .collect(Collectors.toList());
    }

    private ClienteCotaDTO construirClienteCotaIsolado(Empresa empresa) {
        UUID empId = empresa.getId();
        long docsUsados = documentoRepository.countByEmpresaId(empId);
        long bytesUsados = documentoRepository.sumTamanhoBytesByEmpresaId(empId);
        long assistentesUsados = assistenteRepository.countByEmpresaId(empId);
        long usuariosTotal = usuarioRepository.countByEmpresaId(empId);
        long conversasTotal = conversaRepository.countByEmpresaId(empId);

        long inseridosAdmin = creditoTransacaoRepository.findByEmpresaIdOrderByCreatedAtDesc(empId).stream()
                .filter(tx -> tx.getTipo() == TipoTransacaoCredito.INSERCAO_ADMIN
                        || (tx.getTipo() == TipoTransacaoCredito.AJUSTE_ADMIN && tx.getQuantidadeTokens() > 0))
                .mapToLong(tx -> Math.max(0L, tx.getQuantidadeTokens()))
                .sum();

        return montarClienteCotaDTO(
                empresa,
                docsUsados,
                bytesUsados,
                assistentesUsados,
                usuariosTotal,
                conversasTotal,
                inseridosAdmin,
                0L,
                0L,
                0.0,
                empresa.getUpdatedAt()
        );
    }

    private ClienteCotaDTO montarClienteCotaDTO(Empresa empresa,
                                                long docsUsados,
                                                long bytesUsados,
                                                long assistentesUsados,
                                                long usuariosTotal,
                                                long conversasTotal,
                                                long creditosInseridosAdmin,
                                                long requisicoesPeriodo,
                                                long tokensPeriodo,
                                                double custoPeriodoUsd,
                                                LocalDateTime ultimaAtividade) {
        ClienteCotaDTO dto = new ClienteCotaDTO();
        dto.setId(empresa.getId());
        dto.setNome(empresa.getNome());
        dto.setRazaoSocial(empresa.getRazaoSocial());
        dto.setEmail(empresa.getEmail());
        dto.setSlug(empresa.getSlug());
        dto.setStatus(empresa.isStatus());
        dto.setPlano(empresa.getPlano());
        dto.setCreatedAt(empresa.getCreatedAt());
        dto.setUpdatedAt(empresa.getUpdatedAt());

        long saldo = empresa.getCreditosTokens();
        long consumidos = empresa.getTokensConsumidos();
        long totalTokensMovimentados = saldo + consumidos;
        double pctTokensUsados = totalTokensMovimentados > 0
                ? ((double) consumidos / totalTokensMovimentados) * 100.0
                : 0.0;

        dto.setCreditosTokens(saldo);
        dto.setTokensConsumidos(consumidos);
        dto.setCreditosInseridosAdmin(creditosInseridosAdmin);
        dto.setPercentualTokensUsados(Math.round(pctTokensUsados * 10.0) / 10.0);
        dto.setAlertaCreditoBaixo(saldo <= LIMIAR_CREDITO_BAIXO_TOKENS || pctTokensUsados >= LIMIAR_CREDITO_BAIXO_PERCENT);

        int maxArquivos = Math.max(1, empresa.getMaxArquivosPermitidos());
        long limiteBytes = Math.max(1L, empresa.getLimiteArmazenamentoBytes());
        int limiteAssistentes = Math.max(1, empresa.getLimiteAssistentes());

        dto.setDocumentosUsados(docsUsados);
        dto.setMaxArquivos(maxArquivos);
        dto.setPercentualArquivosUsados(Math.min(100.0, Math.round(((double) docsUsados / maxArquivos) * 1000.0) / 10.0));

        dto.setArmazenamentoUsadoBytes(bytesUsados);
        dto.setLimiteArmazenamentoBytes(limiteBytes);
        dto.setPercentualArmazenamentoUsado(Math.min(100.0, Math.round(((double) bytesUsados / limiteBytes) * 1000.0) / 10.0));

        dto.setAssistentesUsados(assistentesUsados);
        dto.setLimiteAssistentes(limiteAssistentes);
        dto.setPercentualAssistentesUsados(Math.min(100.0, Math.round(((double) assistentesUsados / limiteAssistentes) * 1000.0) / 10.0));

        dto.setTotalUsuarios(usuariosTotal);
        dto.setTotalConversas(conversasTotal);
        dto.setRequisicoesPeriodo(requisicoesPeriodo);
        dto.setTokensPeriodo(tokensPeriodo);
        dto.setCustoEstimadoPeriodoUsd(custoPeriodoUsd);
        dto.setUltimaAtividade(ultimaAtividade);
        return dto;
    }

    private CreditoTransacaoDTO toTransacaoDTO(CreditoTransacao entity) {
        CreditoTransacaoDTO dto = new CreditoTransacaoDTO();
        dto.setId(entity.getId());
        dto.setEmpresaId(entity.getEmpresaId());
        dto.setEmpresaNome(entity.getEmpresaNome());
        dto.setTipo(entity.getTipo());
        dto.setQuantidadeTokens(entity.getQuantidadeTokens());
        dto.setSaldoAnterior(entity.getSaldoAnterior());
        dto.setSaldoPosterior(entity.getSaldoPosterior());
        dto.setMotivo(entity.getMotivo());
        dto.setRealizadoPorEmail(entity.getRealizadoPorEmail());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
