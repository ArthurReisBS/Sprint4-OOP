package br.com.motiva.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Positive;

/**
 * Corte mecanico da vegetacao.
 *
 * Unica subclasse com colunas proprias. Elas sao anulaveis na tabela porque a
 * pulverizacao as deixa nulas; o CHECK ck_interv_subtipo exige as duas
 * preenchidas quando TIPO = 'ROCADA'.
 */
@Entity
@DiscriminatorValue("ROCADA")
public class RocadaMecanizada extends IntervencaoOperacional {

    // nulo passa aqui; a obrigatoriedade e checada no Service, porque so vale
    // para a rocada e a coluna e compartilhada com a pulverizacao
    @Positive(message = "Largura da faixa deve ser positiva.")
    @Column(name = "LARGURA_FAIXA_M", columnDefinition = "NUMBER(5,2)")
    private Double larguraFaixaM;

    @Positive(message = "Altura de corte deve ser positiva.")
    @Column(name = "ALTURA_CORTE_CM", columnDefinition = "NUMBER(5,2)")
    private Double alturaCorteCm;

    protected RocadaMecanizada() {
        // exigido pelo JPA
    }

    public RocadaMecanizada(TrechoRodovia trecho, EquipeManutencao equipe,
                            LocalDate dataExecucao, double custo,
                            double larguraFaixaM, double alturaCorteCm) {
        super(trecho, equipe, dataExecucao, custo);
        this.larguraFaixaM = larguraFaixaM;
        this.alturaCorteCm = alturaCorteCm;
    }

    @Override
    public TipoIntervencao getTipo() {
        return TipoIntervencao.ROCADA;
    }

    @Override
    public String descricaoTecnica() {
        return String.format("Roçada de %.1f m de faixa, corte a %.0f cm",
                larguraFaixaM, alturaCorteCm);
    }

    public Double getLarguraFaixaM() {
        return larguraFaixaM;
    }

    public void setLarguraFaixaM(Double larguraFaixaM) {
        this.larguraFaixaM = larguraFaixaM;
    }

    public Double getAlturaCorteCm() {
        return alturaCorteCm;
    }

    public void setAlturaCorteCm(Double alturaCorteCm) {
        this.alturaCorteCm = alturaCorteCm;
    }
}
