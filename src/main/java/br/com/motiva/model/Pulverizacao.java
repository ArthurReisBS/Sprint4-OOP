package br.com.motiva.model;

import java.time.LocalDate;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Intervencao por pulverizacao.
 *
 * Nao tem atributos proprios: a linha se distingue apenas por TIPO =
 * 'PULVERIZACAO', e as colunas da rocada ficam nulas.
 */
@Entity
@DiscriminatorValue("PULVERIZACAO")
public class Pulverizacao extends IntervencaoOperacional {

    protected Pulverizacao() {
        // exigido pelo JPA
    }

    public Pulverizacao(TrechoRodovia trecho, EquipeManutencao equipe,
                        LocalDate dataExecucao, double custo) {
        super(trecho, equipe, dataExecucao, custo);
    }

    @Override
    public TipoIntervencao getTipo() {
        return TipoIntervencao.PULVERIZACAO;
    }

    @Override
    public String descricaoTecnica() {
        return "Pulverização";
    }
}
