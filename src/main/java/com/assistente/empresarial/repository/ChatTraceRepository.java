package com.assistente.empresarial.repository;

import com.assistente.empresarial.model.ChatTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Repositório híbrido (Banco PostgreSQL + Buffer L1 em memória) para armazenamento e consulta
 * persistente de traces de execução de IA (ChatTrace).
 */
@Repository
public class ChatTraceRepository {

    private static final Logger log = LoggerFactory.getLogger(ChatTraceRepository.class);
    private static final int MAX_TRACES_HISTORY = 500;

    private final Deque<ChatTrace> traceBuffer = new ConcurrentLinkedDeque<>();
    private final ChatTraceJpaRepository jpaRepository;

    @Autowired
    public ChatTraceRepository(@Autowired(required = false) ChatTraceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public ChatTraceRepository() {
        this.jpaRepository = null;
    }

    public void save(ChatTrace trace) {
        if (trace == null) return;
        traceBuffer.addFirst(trace);
        while (traceBuffer.size() > MAX_TRACES_HISTORY) {
            traceBuffer.pollLast();
        }
        if (jpaRepository != null) {
            try {
                jpaRepository.save(trace);
            } catch (Exception e) {
                log.warn("Não foi possível persistir ChatTrace no banco PostgreSQL: {}", e.getMessage());
            }
        }
    }

    public List<ChatTrace> findRecent(int limit) {
        int max = limit > 0 ? limit : 50;
        if (jpaRepository != null) {
            try {
                List<ChatTrace> fromDb = jpaRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, max));
                if (fromDb != null && !fromDb.isEmpty()) {
                    return fromDb;
                }
            } catch (Exception e) {
                log.debug("Fallback para buffer em memória ao consultar traces recentes: {}", e.getMessage());
            }
        }
        int count = Math.min(max, traceBuffer.size());
        List<ChatTrace> list = new ArrayList<>(count);
        int i = 0;
        for (ChatTrace t : traceBuffer) {
            if (i++ >= count) break;
            list.add(t);
        }
        return list;
    }

    public Collection<ChatTrace> findAll() {
        if (jpaRepository != null && traceBuffer.isEmpty()) {
            try {
                List<ChatTrace> fromDb = jpaRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, MAX_TRACES_HISTORY));
                if (fromDb != null && !fromDb.isEmpty()) {
                    return fromDb;
                }
            } catch (Exception ignored) {
            }
        }
        return traceBuffer;
    }
}
