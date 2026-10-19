package br.com.motiva.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Trecho de rodovia monitorado.
 *
 * A prioridade nao e coluna nem campo: e derivada da altura, da especie e dos
 * dias decorridos, e quem calcula e o Service.
 */
@Entity
@Table(name = "TRECHO_RODOVIA")
public class TrechoRodovia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "RODOVIA", nullable = false, length = 20)
    private String rodovia;

    // columnDefinition fixa o NUMBER: sem ela o Hibernate espera FLOAT para
    // double e o ddl-auto=validate recusa a coluna
    @Column(name = "KM_INICIAL", nullable = false, columnDefinition = "NUMBER(7,2)")
    private double kmInicial;

    @Column(name = "KM_FINAL", nullable = false, columnDefinition = "NUMBER(7,2)")
    private double kmFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESPECIE_PREDOMINANTE", nullable = false, length = 20)
    private EspecieGrama especiePredominante;

    @Column(name = "ALTURA_VEGETACAO_CM", nullable = false, columnDefinition = "NUMBER(6,2)")
    private double alturaVegetacaoCm;

    @Column(name = "DATA_ULTIMA_MEDICAO", nullable = false)
    private LocalDate dataUltimaMedicao;

    // null = nunca recebeu intervencao
    @Column(name = "DATA_ULTIMA_INTERVENCAO")
    private LocalDate dataUltimaIntervencao;

    protected TrechoRodovia() {
        // exigido pelo JPA
    }

    public TrechoRodovia(String rodovia, double kmInicial, double kmFinal,
                         EspecieGrama especiePredominante, double alturaVegetacaoCm,
                         LocalDate dataUltimaMedicao, LocalDate dataUltimaIntervencao) {
        this.rodovia = rodovia;
        this.kmInicial = kmInicial;
        this.kmFinal = kmFinal;
        this.especiePredominante = especiePredominante;
        this.alturaVegetacaoCm = alturaVegetacaoCm;
        this.dataUltimaMedicao = dataUltimaMedicao;
        this.dataUltimaIntervencao = dataUltimaIntervencao;
    }

    public double extensaoKm() {
        return kmFinal - kmInicial;
    }

    public String identificacao() {
        return String.format("%s km %.1f-%.1f", rodovia, kmInicial, kmFinal);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRodovia() {
        return rodovia;
    }

    public void setRodovia(String rodovia) {
        this.rodovia = rodovia;
    }

    public double getKmInicial() {
        return kmInicial;
    }

    public void setKmInicial(double kmInicial) {
        this.kmInicial = kmInicial;
    }

    public double getKmFinal() {
        return kmFinal;
    }

    public void setKmFinal(double kmFinal) {
        this.kmFinal = kmFinal;
    }

    public EspecieGrama getEspeciePredominante() {
        return especiePredominante;
    }

    public void setEspeciePredominante(EspecieGrama especiePredominante) {
        this.especiePredominante = especiePredominante;
    }

    public double getAlturaVegetacaoCm() {
        return alturaVegetacaoCm;
    }

    public void setAlturaVegetacaoCm(double alturaVegetacaoCm) {
        this.alturaVegetacaoCm = alturaVegetacaoCm;
    }

    public LocalDate getDataUltimaMedicao() {
        return dataUltimaMedicao;
    }

    public void setDataUltimaMedicao(LocalDate dataUltimaMedicao) {
        this.dataUltimaMedicao = dataUltimaMedicao;
    }

    public LocalDate getDataUltimaIntervencao() {
        return dataUltimaIntervencao;
    }

    public void setDataUltimaIntervencao(LocalDate dataUltimaIntervencao) {
        this.dataUltimaIntervencao = dataUltimaIntervencao;
    }
}
