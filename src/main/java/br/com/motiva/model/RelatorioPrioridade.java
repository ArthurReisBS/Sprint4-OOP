package br.com.motiva.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Um relatorio gerado, com os contadores por faixa.
 *
 * Guarda os totais, e nao a lista de trechos: o detalhe de cada intervencao
 * fica em INTERVENCAO_OPERACIONAL.
 */
@Entity
@Table(name = "RELATORIO_PRIORIDADE")
public class RelatorioPrioridade {

    /** Limite da coluna RESUMO. */
    public static final int TAMANHO_MAXIMO_RESUMO = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "DATA_GERACAO", nullable = false)
    private LocalDateTime dataGeracao;

    @Column(name = "QT_URGENTE", nullable = false, columnDefinition = "NUMBER(5)")
    private int qtUrgente;

    @Column(name = "QT_CRITICO", nullable = false, columnDefinition = "NUMBER(5)")
    private int qtCritico;

    @Column(name = "QT_ATENCAO", nullable = false, columnDefinition = "NUMBER(5)")
    private int qtAtencao;

    @Column(name = "QT_NORMAL", nullable = false, columnDefinition = "NUMBER(5)")
    private int qtNormal;

    @Column(name = "RESUMO", length = TAMANHO_MAXIMO_RESUMO)
    private String resumo;

    protected RelatorioPrioridade() {
        // exigido pelo JPA
    }

    public RelatorioPrioridade(int qtUrgente, int qtCritico, int qtAtencao, int qtNormal,
                               String resumo) {
        this.qtUrgente = qtUrgente;
        this.qtCritico = qtCritico;
        this.qtAtencao = qtAtencao;
        this.qtNormal = qtNormal;
        this.resumo = resumo;
    }

    // a coluna tem DEFAULT SYSTIMESTAMP, mas o Hibernate manda o campo no INSERT
    // mesmo nulo, e o NULL explicito passa por cima do default
    @PrePersist
    void preencherDataGeracao() {
        if (dataGeracao == null) {
            dataGeracao = LocalDateTime.now();
        }
    }

    public int totalTrechos() {
        return qtUrgente + qtCritico + qtAtencao + qtNormal;
    }

    /** Trechos que exigem acao: urgentes mais criticos. */
    public int totalAcionaveis() {
        return qtUrgente + qtCritico;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataGeracao() {
        return dataGeracao;
    }

    public void setDataGeracao(LocalDateTime dataGeracao) {
        this.dataGeracao = dataGeracao;
    }

    public int getQtUrgente() {
        return qtUrgente;
    }

    public void setQtUrgente(int qtUrgente) {
        this.qtUrgente = qtUrgente;
    }

    public int getQtCritico() {
        return qtCritico;
    }

    public void setQtCritico(int qtCritico) {
        this.qtCritico = qtCritico;
    }

    public int getQtAtencao() {
        return qtAtencao;
    }

    public void setQtAtencao(int qtAtencao) {
        this.qtAtencao = qtAtencao;
    }

    public int getQtNormal() {
        return qtNormal;
    }

    public void setQtNormal(int qtNormal) {
        this.qtNormal = qtNormal;
    }

    public String getResumo() {
        return resumo;
    }

    public void setResumo(String resumo) {
        this.resumo = resumo;
    }
}
