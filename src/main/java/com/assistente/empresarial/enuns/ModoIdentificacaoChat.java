package com.assistente.empresarial.enuns;

public enum ModoIdentificacaoChat {
    NENHUM,
    NOME_APENAS,
    NOME_CONTATO_OPCIONAL,
    NOME_EMAIL_OBRIGATORIO;

    public static ModoIdentificacaoChat fromString(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return NOME_CONTATO_OPCIONAL;
        }
        try {
            return ModoIdentificacaoChat.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NOME_CONTATO_OPCIONAL;
        }
    }
}
