package br.com.motiva.model;

import org.hibernate.type.NumericBooleanConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Equipe de campo que executa as intervencoes.
 */
@Entity
@Table(name = "EQUIPE_MANUTENCAO")
public class EquipeManutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NOME", nullable = false, length = 80)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 20)
    private TipoEquipe tipo;

    @Column(name = "QT_INTEGRANTES", nullable = false, columnDefinition = "NUMBER(3)")
    private int quantidadeIntegrantes;

    // a coluna e NUMBER(1), 0 ou 1. O conversor faz o Hibernate gravar e
    // comparar 0/1 em qualquer banco: sem ele, findByDisponivelTrue vira
    // "= true", e o NUMBER nao se compara com BOOLEAN
    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "DISPONIVEL", nullable = false, columnDefinition = "NUMBER(1)")
    private boolean disponivel = true;

    protected EquipeManutencao() {
        // exigido pelo JPA
    }

    public EquipeManutencao(String nome, TipoEquipe tipo,
                            int quantidadeIntegrantes, boolean disponivel) {
        this.nome = nome;
        this.tipo = tipo;
        this.quantidadeIntegrantes = quantidadeIntegrantes;
        this.disponivel = disponivel;
    }

    /** True se a equipe esta livre e sabe executar o servico. */
    public boolean podeAtender(TipoIntervencao tipoIntervencao) {
        return disponivel && tipo.podeExecutar(tipoIntervencao);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoEquipe getTipo() {
        return tipo;
    }

    public void setTipo(TipoEquipe tipo) {
        this.tipo = tipo;
    }

    public int getQuantidadeIntegrantes() {
        return quantidadeIntegrantes;
    }

    public void setQuantidadeIntegrantes(int quantidadeIntegrantes) {
        this.quantidadeIntegrantes = quantidadeIntegrantes;
    }

    public boolean isDisponivel() {
        return disponivel;
    }

    public void setDisponivel(boolean disponivel) {
        this.disponivel = disponivel;
    }
}
