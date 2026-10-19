package br.com.motiva.dto;

import java.time.LocalDate;

import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.model.RocadaMecanizada;
import br.com.motiva.model.TipoIntervencao;

/**
 * Intervencao como sai na API: trecho e equipe resumidos em ID e nome, em vez
 * das entidades inteiras aninhadas em cada item do historico.
 */
public record IntervencaoResponse(
        Long id,
        TipoIntervencao tipo,
        String descricao,
        Long trechoId,
        String trecho,
        Long equipeId,
        String equipe,
        LocalDate dataExecucao,
        double custo,
        Double larguraFaixaM,
        Double alturaCorteCm
) {

    public static IntervencaoResponse de(IntervencaoOperacional intervencao) {
        Double largura = null;
        Double alturaCorte = null;
        if (intervencao instanceof RocadaMecanizada rocada) {
            largura = rocada.getLarguraFaixaM();
            alturaCorte = rocada.getAlturaCorteCm();
        }
        return new IntervencaoResponse(
                intervencao.getId(),
                intervencao.getTipo(),
                intervencao.descricaoTecnica(),
                intervencao.getTrecho().getId(),
                intervencao.getTrecho().identificacao(),
                intervencao.getEquipe().getId(),
                intervencao.getEquipe().getNome(),
                intervencao.getDataExecucao(),
                intervencao.getCusto(),
                largura,
                alturaCorte);
    }
}
