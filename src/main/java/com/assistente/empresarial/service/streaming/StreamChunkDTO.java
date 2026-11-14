package com.assistente.empresarial.service.streaming;

import java.util.Map;

public class StreamChunkDTO {

    private String type; // "token", "fontes", "tool", "done", "error"
    private String content;
    private Map<String, Object> metadata;

    public StreamChunkDTO() {
    }

    public StreamChunkDTO(String type, String content, Map<String, Object> metadata) {
        this.type = type;
        this.content = content;
        this.metadata = metadata;
    }

    public static StreamChunkDTO token(String text) {
        return new StreamChunkDTO("token", text, null);
    }

    public static StreamChunkDTO fontes(Map<String, Object> metadata) {
        return new StreamChunkDTO("fontes", null, metadata);
    }

    public static StreamChunkDTO tool(String toolName) {
        return new StreamChunkDTO("tool", toolName, null);
    }

    public static StreamChunkDTO done(Map<String, Object> metadata) {
        return new StreamChunkDTO("done", null, metadata);
    }

    public static StreamChunkDTO error(String errorMessage) {
        return new StreamChunkDTO("error", errorMessage, null);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
