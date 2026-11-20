package com.assistente.empresarial.service;

import com.assistente.empresarial.dto.TransbordoEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Motor de Transbordo Humano em Tempo Real via Server-Sent Events (SSE).
 * - Mantém canais de streaming abertos para o Painel de Atendimento Humano da Empresa.
 * - Mantém canais SSE por conversa para que o Widget do cliente receba respostas do atendente humano instantaneamente.
 */
@Service
public class HumanHandoffService {

    private static final Logger log = LoggerFactory.getLogger(HumanHandoffService.class);
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutos

    private final Map<UUID, List<SseEmitter>> paineisEmpresa = new ConcurrentHashMap<>();
    private final Map<UUID, List<SseEmitter>> canaisConversa = new ConcurrentHashMap<>();

    public SseEmitter conectarPainelEmpresa(UUID empresaId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        List<SseEmitter> lista = paineisEmpresa.computeIfAbsent(empresaId, k -> new CopyOnWriteArrayList<>());
        lista.add(emitter);

        emitter.onCompletion(() -> lista.remove(emitter));
        emitter.onTimeout(() -> lista.remove(emitter));
        emitter.onError(e -> lista.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("CONECTADO")
                    .data(Map.of("status", "ONLINE", "empresaId", empresaId.toString()), MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            lista.remove(emitter);
        }

        return emitter;
    }

    public SseEmitter conectarConversaCliente(UUID conversaId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        List<SseEmitter> lista = canaisConversa.computeIfAbsent(conversaId, k -> new CopyOnWriteArrayList<>());
        lista.add(emitter);

        emitter.onCompletion(() -> lista.remove(emitter));
        emitter.onTimeout(() -> lista.remove(emitter));
        emitter.onError(e -> lista.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("CONECTADO")
                    .data(Map.of("status", "CONECTADO", "conversaId", conversaId.toString()), MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            lista.remove(emitter);
        }

        return emitter;
    }

    public void notificarEvento(TransbordoEventDTO evento) {
        if (evento == null) return;

        if (evento.getEmpresaId() != null) {
            List<SseEmitter> emittersEmpresa = paineisEmpresa.get(evento.getEmpresaId());
            if (emittersEmpresa != null) {
                for (SseEmitter emitter : emittersEmpresa) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(evento.getTipoEvento())
                                .data(evento, MediaType.APPLICATION_JSON));
                    } catch (Exception e) {
                        emittersEmpresa.remove(emitter);
                    }
                }
            }
        }

        if (evento.getConversaId() != null) {
            List<SseEmitter> emittersConversa = canaisConversa.get(evento.getConversaId());
            if (emittersConversa != null) {
                for (SseEmitter emitter : emittersConversa) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(evento.getTipoEvento())
                                .data(evento, MediaType.APPLICATION_JSON));
                    } catch (Exception e) {
                        emittersConversa.remove(emitter);
                    }
                }
            }
        }
    }
}
