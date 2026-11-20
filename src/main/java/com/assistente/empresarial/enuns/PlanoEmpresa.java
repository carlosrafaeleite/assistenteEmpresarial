package com.assistente.empresarial.enuns;

public enum PlanoEmpresa {
    SIMPLES(1, 30L * 1024 * 1024, 100_000L),
    EMPRESARIAL(5, 50L * 1024 * 1024, 500_000L);

    private final int maxArquivos;
    private final long maxBytesTotal;
    private final long creditosIniciais;

    PlanoEmpresa(int maxArquivos, long maxBytesTotal, long creditosIniciais) {
        this.maxArquivos = maxArquivos;
        this.maxBytesTotal = maxBytesTotal;
        this.creditosIniciais = creditosIniciais;
    }

    public int getMaxArquivos() {
        return maxArquivos;
    }

    public long getMaxBytesTotal() {
        return maxBytesTotal;
    }

    public long getMaxMegabytes() {
        return maxBytesTotal / (1024 * 1024);
    }

    public long getCreditosIniciais() {
        return creditosIniciais;
    }

    public static PlanoEmpresa fromString(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return SIMPLES;
        }
        try {
            return PlanoEmpresa.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SIMPLES;
        }
    }
}
