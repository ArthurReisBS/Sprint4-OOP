package br.com.motiva.model;

/**
 * Faixa de urgencia de um trecho.
 *
 * So descreve as faixas: a classificacao de uma altura em faixa e regra de
 * negocio e mora no Service.
 */
public enum Prioridade {

    URGENTE("Passou da altura de corte recomendada: intervir imediatamente"),
    CRITICO("Corte necessário: agendar para esta semana"),
    ATENCAO("Dentro do limite, mas subindo: monitorar"),
    NORMAL ("Sem necessidade de corte");

    public static final double FAIXA_VERDE_MAX_CM = 15.0;
    public static final double FAIXA_AMARELA_MAX_CM = 25.0;
    public static final double ALTURA_CORTE_RECOMENDADO_CM = 30.0;

    private final String recomendacao;

    Prioridade(String recomendacao) {
        this.recomendacao = recomendacao;
    }

    public String getRecomendacao() {
        return recomendacao;
    }

    /** Cor equivalente no mapa operacional. */
    public String corNoMapa() {
        return switch (this) {
            case URGENTE, CRITICO -> "vermelho";
            case ATENCAO          -> "amarelo";
            case NORMAL           -> "verde";
        };
    }

    /** True se o trecho entra no cronograma da semana. */
    public boolean exigeIntervencao() {
        return this == URGENTE || this == CRITICO;
    }
}
