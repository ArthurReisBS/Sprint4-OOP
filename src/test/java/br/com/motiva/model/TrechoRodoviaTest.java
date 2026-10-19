package br.com.motiva.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrechoRodoviaTest {

    private Locale localeOriginal;

    // identificacao() usa String.format, que depende do locale da JVM
    @BeforeEach
    void fixarLocale() {
        localeOriginal = Locale.getDefault();
        Locale.setDefault(new Locale("pt", "BR"));
    }

    @AfterEach
    void restaurarLocale() {
        Locale.setDefault(localeOriginal);
    }

    private TrechoRodovia trecho(double kmInicial, double kmFinal) {
        return new TrechoRodovia("SP-021", kmInicial, kmFinal, EspecieGrama.PASPALUM,
                22.0, LocalDate.now(), null);
    }

    @Test
    void extensaoEADiferencaEntreOsKms() {
        assertThat(trecho(12.0, 18.5).extensaoKm()).isEqualTo(6.5);
    }

    @Test
    void identificacaoTrazRodoviaEFaixa() {
        assertThat(trecho(12.0, 18.5).identificacao()).isEqualTo("SP-021 km 12,0-18,5");
    }

    @Test
    void trechoNovoNaoTemIdNemIntervencao() {
        TrechoRodovia novo = trecho(12.0, 18.5);

        assertThat(novo.getId()).isNull();
        assertThat(novo.getDataUltimaIntervencao()).isNull();
    }
}
