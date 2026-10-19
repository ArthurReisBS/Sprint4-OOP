package br.com.motiva.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.model.Pulverizacao;
import br.com.motiva.model.RocadaMecanizada;
import br.com.motiva.model.TipoIntervencao;

/**
 * Corpo do POST e do PUT de intervencao.
 *
 * Existe porque IntervencaoOperacional e abstrata: o Jackson nao saberia qual
 * subclasse instanciar. O campo tipo decide, e trecho e equipe chegam so pelo
 * ID, sem o cliente ter de mandar os objetos inteiros.
 */
public record IntervencaoRequest(
        @NotNull(message = "Tipo da intervenção é obrigatório (ROCADA ou PULVERIZACAO).")
        TipoIntervencao tipo,

        @NotNull(message = "Trecho é obrigatório.")
        Long trechoId,

        @NotNull(message = "Equipe é obrigatória.")
        Long equipeId,

        @NotNull(message = "Data de execução é obrigatória.")
        @PastOrPresent(message = "Data de execução não pode estar no futuro.")
        LocalDate dataExecucao,

        @PositiveOrZero(message = "Custo não pode ser negativo.")
        double custo,

        @Positive(message = "Largura da faixa deve ser positiva.")
        Double larguraFaixaM,

        @Positive(message = "Altura de corte deve ser positiva.")
        Double alturaCorteCm
) {

    /** A subclasse certa, ainda sem trecho e equipe: quem vincula e o Service. */
    public IntervencaoOperacional paraEntidade() {
        if (tipo == null) {
            throw new RegraNegocioException("Tipo da intervenção é obrigatório (ROCADA ou PULVERIZACAO).");
        }
        return switch (tipo) {
            case ROCADA -> {
                RocadaMecanizada rocada = new RocadaMecanizada(null, null, dataExecucao, custo, 0, 0);
                rocada.setLarguraFaixaM(larguraFaixaM);
                rocada.setAlturaCorteCm(alturaCorteCm);
                yield rocada;
            }
            case PULVERIZACAO -> {
                // o CHECK ck_interv_subtipo exige as colunas da rocada nulas
                if (larguraFaixaM != null || alturaCorteCm != null) {
                    throw new RegraNegocioException(
                            "Pulverização não tem largura de faixa nem altura de corte.");
                }
                yield new Pulverizacao(null, null, dataExecucao, custo);
            }
        };
    }
}
