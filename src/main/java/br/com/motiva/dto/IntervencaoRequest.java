package br.com.motiva.dto;

import java.time.LocalDate;

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
        TipoIntervencao tipo,
        Long trechoId,
        Long equipeId,
        LocalDate dataExecucao,
        double custo,
        Double larguraFaixaM,
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
