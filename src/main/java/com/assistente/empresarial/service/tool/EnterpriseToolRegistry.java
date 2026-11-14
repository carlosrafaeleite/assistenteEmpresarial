package com.assistente.empresarial.service.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EnterpriseToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseToolRegistry.class);

    private final Map<String, EnterpriseTool> toolMap = new HashMap<>();
    private final List<ToolSpecification> specifications = new ArrayList<>();

    public EnterpriseToolRegistry(List<EnterpriseTool> tools) {
        if (tools != null) {
            for (EnterpriseTool tool : tools) {
                toolMap.put(tool.getName(), tool);
                specifications.add(tool.getSpecification());
                log.info("Ferramenta corporativa registrada: '{}' - {}", tool.getName(), tool.getDescription());
            }
        }
    }

    public List<ToolSpecification> getAllSpecifications() {
        return Collections.unmodifiableList(specifications);
    }

    public Optional<EnterpriseTool> findByName(String name) {
        return Optional.ofNullable(toolMap.get(name));
    }

    public boolean hasTool(String name) {
        return toolMap.containsKey(name);
    }
}
