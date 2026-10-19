package br.com.motiva.model;

/**
 * Valores da coluna TIPO de INTERVENCAO_OPERACIONAL. Os nomes das constantes
 * sao os mesmos dos @DiscriminatorValue das subclasses.
 */
public enum TipoIntervencao {

    ROCADA("Roçada mecanizada"),
    PULVERIZACAO("Pulverização");

    private final String descricao;

    TipoIntervencao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
