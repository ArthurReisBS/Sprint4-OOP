package br.com.motiva.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.motiva.model.EspecieGrama;
import br.com.motiva.model.TrechoRodovia;

@Repository
public interface TrechoRodoviaRepository extends JpaRepository<TrechoRodovia, Long> {

    // WHERE rodovia = ? ORDER BY km_inicial
    List<TrechoRodovia> findByRodoviaOrderByKmInicialAsc(String rodovia);

    // WHERE especie_predominante = ?
    List<TrechoRodovia> findByEspeciePredominante(EspecieGrama especie);

    // WHERE altura_vegetacao_cm >= ?  (altura medida, nao a estimada: essa e do Service)
    List<TrechoRodovia> findByAlturaVegetacaoCmGreaterThanEqual(double alturaMinimaCm);

    // WHERE data_ultima_intervencao IS NULL
    List<TrechoRodovia> findByDataUltimaIntervencaoIsNull();

    // a UNIQUE uk_trecho_faixa: o Service consulta antes de inserir para devolver
    // um erro claro em vez da violacao de constraint do Oracle
    boolean existsByRodoviaAndKmInicialAndKmFinal(String rodovia, double kmInicial, double kmFinal);
}
