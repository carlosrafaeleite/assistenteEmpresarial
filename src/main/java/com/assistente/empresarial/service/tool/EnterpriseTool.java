package com.assistente.empresarial.service.tool;

import dev.langchain4j.agent.tool.ToolSpecification;

public interface EnterpriseTool {

    String getName();

    String getDescription();

    ToolSpecification getSpecification();

    String execute(String jsonArguments);
}
