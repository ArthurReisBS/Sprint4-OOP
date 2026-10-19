package br.com.motiva.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Intervencao executada em um trecho.
 *
 * Na Sprint 3 era uma interface sealed implementada por records. O JPA precisa
 * de classe com construtor vazio, entao virou classe abstrata: RocadaMecanizada
 * e Pulverizacao continuam dividindo INTERVENCAO_OPERACIONAL, e a coluna TIPO
 * diz qual subclasse o Hibernate instancia ao ler cada linha.
 */
@Entity
@Table(name = "INTERVENCAO_OPERACIONAL")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TIPO", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class IntervencaoOperacional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "TRECHO_ID", nullable = false)
    private TrechoRodovia trecho;

    @ManyToOne(optional = false)
    @JoinColumn(name = "EQUIPE_ID", nullable = false)
    private EquipeManutencao equipe;

    @Column(name = "DATA_EXECUCAO", nullable = false)
    private LocalDate dataExecucao;

    @Column(name = "CUSTO", nullable = false, columnDefinition = "NUMBER(10,2)")
    private double custo;

    protected IntervencaoOperacional() {
        // exigido pelo JPA
    }

    protected IntervencaoOperacional(TrechoRodovia trecho, EquipeManutencao equipe,
                                     LocalDate dataExecucao, double custo) {
        this.trecho = trecho;
        this.equipe = equipe;
        this.dataExecucao = dataExecucao;
        this.custo = custo;
    }

    /** Valor da coluna TIPO; nao e campo porque o Hibernate ja grava pelo discriminador. */
    public abstract TipoIntervencao getTipo();

    /** Linha unica descrevendo o servico. */
    public abstract String descricaoTecnica();

    public double custoPorKm() {
        return custo / trecho.extensaoKm();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TrechoRodovia getTrecho() {
        return trecho;
    }

    public void setTrecho(TrechoRodovia trecho) {
        this.trecho = trecho;
    }

    public EquipeManutencao getEquipe() {
        return equipe;
    }

    public void setEquipe(EquipeManutencao equipe) {
        this.equipe = equipe;
    }

    public LocalDate getDataExecucao() {
        return dataExecucao;
    }

    public void setDataExecucao(LocalDate dataExecucao) {
        this.dataExecucao = dataExecucao;
    }

    public double getCusto() {
        return custo;
    }

    public void setCusto(double custo) {
        this.custo = custo;
    }
}
