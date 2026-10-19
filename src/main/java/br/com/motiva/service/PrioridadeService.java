package br.com.motiva.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.EspecieGrama;
import br.com.motiva.model.Prioridade;
import br.com.motiva.model.TrechoRodovia;

/**
 * Motor de prioridade: estima a altura atual de cada trecho e a classifica
 * em faixa.
 *
 * Nao toca no banco. Os outros Services passam os trechos ja carregados, o
 * que permite testar a classificacao sem Oracle.
 */
@Service
public class PrioridadeService {

    /**
     * Altura provavel hoje, no pior caso: medida + taxa da especie x dias,
     * limitada ao porte maximo da especie.
     *
     * O teto evita a extrapolacao linear absurda em trechos sem medicao
     * recente.
     */
    public double alturaEstimadaCm(TrechoRodovia trecho, LocalDate hoje) {
        EspecieGrama especie = trecho.getEspeciePredominante();
        long dias = Math.max(0, ChronoUnit.DAYS.between(trecho.getDataUltimaMedicao(), hoje));
        double crescimento = especie.getTaxaBaseCmDia() * dias;
        return Math.min(trecho.getAlturaVegetacaoCm() + crescimento, especie.getAlturaMaximaCm());
    }

    public double alturaEstimadaCm(TrechoRodovia trecho) {
        return alturaEstimadaCm(trecho, LocalDate.now());
    }

    /**
     * Classifica uma altura em cm.
     *
     * Os limiares intermediarios sao exclusivos: 15,0 cm ainda e NORMAL e
     * 25,0 cm ainda e ATENCAO. So o de corte recomendado e inclusivo.
     */
    public Prioridade classificar(double alturaCm) {
        if (alturaCm < 0) {
            throw new RegraNegocioException("Altura negativa: " + alturaCm);
        }
        if (alturaCm >= Prioridade.ALTURA_CORTE_RECOMENDADO_CM) {
            return Prioridade.URGENTE;
        }
        if (alturaCm > Prioridade.FAIXA_AMARELA_MAX_CM) {
            return Prioridade.CRITICO;
        }
        if (alturaCm > Prioridade.FAIXA_VERDE_MAX_CM) {
            return Prioridade.ATENCAO;
        }
        return Prioridade.NORMAL;
    }

    public Prioridade prioridadeDo(TrechoRodovia trecho) {
        return classificar(alturaEstimadaCm(trecho));
    }

    /** Dias restantes ate a altura de corte recomendada. Zero se ja passou. */
    public long diasAteCorte(TrechoRodovia trecho) {
        double folga = Prioridade.ALTURA_CORTE_RECOMENDADO_CM - alturaEstimadaCm(trecho);
        if (folga <= 0) {
            return 0;
        }
        return (long) Math.ceil(folga / trecho.getEspeciePredominante().getTaxaBaseCmDia());
    }

    /**
     * Agrupa os trechos por faixa.
     *
     * Todas as faixas entram, mesmo vazias, e dentro de cada uma o trecho mais
     * alto vem primeiro: e a ordem de atendimento.
     */
    public Map<Prioridade, List<TrechoRodovia>> agruparPorPrioridade(List<TrechoRodovia> trechos) {
        // EnumMap mantem a ordem de declaracao do enum, que e a do relatorio
        Map<Prioridade, List<TrechoRodovia>> mapa = new EnumMap<>(Prioridade.class);
        for (Prioridade p : Prioridade.values()) {
            mapa.put(p, new ArrayList<>());
        }

        LocalDate hoje = LocalDate.now();
        for (TrechoRodovia trecho : trechos) {
            mapa.get(classificar(alturaEstimadaCm(trecho, hoje))).add(trecho);
        }

        Comparator<TrechoRodovia> maisAltoPrimeiro =
                Comparator.comparingDouble((TrechoRodovia t) -> alturaEstimadaCm(t, hoje)).reversed();
        mapa.values().forEach(lista -> lista.sort(maisAltoPrimeiro));

        return mapa;
    }
}
