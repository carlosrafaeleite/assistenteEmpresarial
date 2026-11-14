package com.assistente.empresarial.repository;

import com.assistente.empresarial.model.ChatTrace;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Repositório para armazenamento e consulta de traces de execução de IA (ChatTrace).
 */
@Repository
public class ChatTraceRepository {

    private static final int MAX_TRACES_HISTORY = 500;

    private final Deque<ChatTrace> traceBuffer = new ConcurrentLinkedDeque<>();

    public void save(ChatTrace trace) {
        if (trace == null) return;
        traceBuffer.addFirst(trace);
        while (traceBuffer.size() > MAX_TRACES_HISTORY) {
            traceBuffer.pollLast();
        }
    }

    public List<ChatTrace> findRecent(int limit) {
        int count = Math.min(limit > 0 ? limit : 50, traceBuffer.size());
        List<ChatTrace> list = new ArrayList<>(count);
        int i = 0;
        for (ChatTrace t : traceBuffer) {
            if (i++ >= count) break;
            list.add(t);
        }
        return list;
    }

    public Collection<ChatTrace> findAll() {
        return traceBuffer;
    }
}
