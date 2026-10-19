package br.com.motiva.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * A API inteira, do HTTP ao banco (H2 no perfil test): confere os codigos de
 * status de cada endpoint e o corpo de erro do GlobalExceptionHandler.
 *
 * Cada teste roda numa transacao desfeita no fim, entao um nao ve os dados do
 * outro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private final String hoje = LocalDate.now().toString();
    private final String oitoDiasAtras = LocalDate.now().minusDays(8).toString();

    private String trechoJson(double kmInicial, double alturaCm) {
        return """
                {"rodovia": "SP-021", "kmInicial": %s, "kmFinal": %s,
                 "especiePredominante": "PASPALUM", "alturaVegetacaoCm": %s,
                 "dataUltimaMedicao": "%s"}
                """.formatted(kmInicial, kmInicial + 6.5, alturaCm, oitoDiasAtras);
    }

    private long criar(String url, String json) throws Exception {
        String corpo = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private long criarTrecho() throws Exception {
        return criar("/api/trechos", trechoJson(12.0, 22.0));
    }

    private long criarEquipe(String tipo) throws Exception {
        return criar("/api/equipes", """
                {"nome": "Equipe %s", "tipo": "%s", "quantidadeIntegrantes": 5, "disponivel": true}
                """.formatted(tipo, tipo));
    }

    private String rocadaJson(long trechoId, long equipeId) {
        return """
                {"tipo": "ROCADA", "trechoId": %d, "equipeId": %d, "dataExecucao": "%s",
                 "custo": 4850.0, "larguraFaixaM": 3.5, "alturaCorteCm": 10.0}
                """.formatted(trechoId, equipeId, hoje);
    }

    // ---------------------------------------------------------------- trechos

    @Test
    void postDeTrechoDevolve201ComOIdGeradoPeloBanco() throws Exception {
        String comIdDoCliente = trechoJson(12.0, 22.0).replace("{", "{\"id\": 999, ");

        mvc.perform(post("/api/trechos").contentType(MediaType.APPLICATION_JSON).content(comIdDoCliente))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.rodovia").value("SP-021"))
                .andExpect(jsonPath("$.dataUltimaMedicao").value(oitoDiasAtras));
    }

    @Test
    void getDeTrechoDevolve200() throws Exception {
        long id = criarTrecho();

        mvc.perform(get("/api/trechos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kmInicial").value(12.0));
        mvc.perform(get("/api/trechos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getDeTrechoInexistenteDevolve404ComCorpoDeErro() throws Exception {
        mvc.perform(get("/api/trechos/{id}", 4242))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value(startsWith("Trecho 4242")))
                .andExpect(jsonPath("$.caminho").value("/api/trechos/4242"));
    }

    @Test
    void trechoComAlturaNegativaDevolve400() throws Exception {
        mvc.perform(post("/api/trechos").contentType(MediaType.APPLICATION_JSON)
                        .content(trechoJson(12.0, -3.0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(startsWith("Altura da vegetação não pode ser negativa")));
    }

    @Test
    void validRespondeTodosOsCamposInvalidosDeUmaVez() throws Exception {
        mvc.perform(post("/api/trechos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"rodovia": "", "kmInicial": 12.0, "kmFinal": 18.5,
                         "alturaVegetacaoCm": -3.0, "dataUltimaMedicao": "2999-01-01"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Altura da vegetação não pode ser negativa. "
                        + "Data da última medição não pode estar no futuro. "
                        + "Espécie predominante é obrigatória. "
                        + "Rodovia é obrigatória."));
    }

    @Test
    void especieQueNaoExisteDevolve400() throws Exception {
        mvc.perform(post("/api/trechos").contentType(MediaType.APPLICATION_JSON)
                        .content(trechoJson(12.0, 22.0).replace("PASPALUM", "GRAMA_AZUL")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void idQueNaoENumeroDevolve400() throws Exception {
        mvc.perform(get("/api/trechos/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void trechosPorPrioridadeUsamAAlturaEstimada() throws Exception {
        criarTrecho(); // 22 cm há 8 dias de Paspalum: 30,8 cm hoje

        mvc.perform(get("/api/trechos/prioridade/URGENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/trechos/prioridade/NORMAL"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void putDeTrechoDevolve200Ou404() throws Exception {
        long id = criarTrecho();

        mvc.perform(put("/api/trechos/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(trechoJson(12.0, 2.0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.alturaVegetacaoCm").value(2.0));
        mvc.perform(put("/api/trechos/{id}", 4242).contentType(MediaType.APPLICATION_JSON)
                        .content(trechoJson(12.0, 2.0)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDeTrechoDevolve204Ou404() throws Exception {
        long id = criarTrecho();

        mvc.perform(delete("/api/trechos/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/trechos/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/trechos/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteDeTrechoComHistoricoDevolve400() throws Exception {
        long trecho = criarTrecho();
        criar("/api/intervencoes", rocadaJson(trecho, criarEquipe("ROCADA")));

        mvc.perform(delete("/api/trechos/{id}", trecho))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(startsWith("Trecho " + trecho + " tem intervenções")));
    }

    // ---------------------------------------------------------------- equipes

    @Test
    void crudDeEquipe() throws Exception {
        long id = criarEquipe("ROCADA");
        criarEquipe("PULVERIZACAO");

        mvc.perform(get("/api/equipes").param("tipo", "ROCADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].disponivel").value(true));
        mvc.perform(put("/api/equipes/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome": "Equipe Delta", "tipo": "MISTA", "quantidadeIntegrantes": 8, "disponivel": false}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("MISTA"));
        mvc.perform(get("/api/equipes/disponiveis"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(delete("/api/equipes/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    void equipeSemIntegrantesDevolve400() throws Exception {
        mvc.perform(post("/api/equipes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome": "Equipe Vazia", "tipo": "ROCADA", "quantidadeIntegrantes": 0}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Quantidade de integrantes deve ser de 1 a 999."));
    }

    @Test
    void equipeSemOCampoDisponivelNasceDisponivel() throws Exception {
        mvc.perform(post("/api/equipes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome": "Equipe Eco", "tipo": "MISTA", "quantidadeIntegrantes": 3}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.disponivel").value(true));
    }

    // ----------------------------------------------------------- intervencoes

    @Test
    void postDeRocadaDevolve201ComTrechoEEquipeResumidos() throws Exception {
        long trecho = criarTrecho();
        long equipe = criarEquipe("ROCADA");

        mvc.perform(post("/api/intervencoes").contentType(MediaType.APPLICATION_JSON)
                        .content(rocadaJson(trecho, equipe)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ROCADA"))
                .andExpect(jsonPath("$.trechoId").value(trecho))
                .andExpect(jsonPath("$.trecho").value(startsWith("SP-021 km 12")))
                .andExpect(jsonPath("$.equipe").value("Equipe ROCADA"))
                .andExpect(jsonPath("$.larguraFaixaM").value(3.5));
    }

    @Test
    void postDePulverizacaoVoltaSemColunasDaRocada() throws Exception {
        long trecho = criarTrecho();
        long equipe = criarEquipe("PULVERIZACAO");

        mvc.perform(post("/api/intervencoes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipo": "PULVERIZACAO", "trechoId": %d, "equipeId": %d,
                         "dataExecucao": "%s", "custo": 3200.0}
                        """.formatted(trecho, equipe, hoje)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value("Pulverização"))
                .andExpect(jsonPath("$.larguraFaixaM").value(nullValue()));
    }

    @Test
    void equipeIncompativelDevolve400() throws Exception {
        long trecho = criarTrecho();
        long pulverizacao = criarEquipe("PULVERIZACAO");

        mvc.perform(post("/api/intervencoes").contentType(MediaType.APPLICATION_JSON)
                        .content(rocadaJson(trecho, pulverizacao)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pulverizacaoComLarguraDeFaixaDevolve400() throws Exception {
        long trecho = criarTrecho();
        long equipe = criarEquipe("PULVERIZACAO");

        mvc.perform(post("/api/intervencoes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipo": "PULVERIZACAO", "trechoId": %d, "equipeId": %d,
                         "dataExecucao": "%s", "custo": 3200.0, "larguraFaixaM": 3.0}
                        """.formatted(trecho, equipe, hoje)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void intervencaoSemTipoDevolve400() throws Exception {
        mvc.perform(post("/api/intervencoes").contentType(MediaType.APPLICATION_JSON)
                        .content(rocadaJson(1, 1).replace("\"tipo\": \"ROCADA\", ", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void intervencaoComCustoNegativoEDataFuturaDevolve400() throws Exception {
        mvc.perform(post("/api/intervencoes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipo": "PULVERIZACAO", "trechoId": 1, "equipeId": 1,
                         "dataExecucao": "2999-01-01", "custo": -1.0}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(
                        "Custo não pode ser negativo. Data de execução não pode estar no futuro."));
    }

    @Test
    void putDeEquipeComNomeEmBrancoDevolve400() throws Exception {
        long id = criarEquipe("ROCADA");

        mvc.perform(put("/api/equipes/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome": " ", "tipo": "ROCADA", "quantidadeIntegrantes": 5}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Nome da equipe é obrigatório."));
    }

    @Test
    void intervencaoEmTrechoInexistenteDevolve404() throws Exception {
        long equipe = criarEquipe("ROCADA");

        mvc.perform(post("/api/intervencoes").contentType(MediaType.APPLICATION_JSON)
                        .content(rocadaJson(4242, equipe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void historicoPutEDeleteDeIntervencao() throws Exception {
        long trecho = criarTrecho();
        long equipe = criarEquipe("MISTA");
        long id = criar("/api/intervencoes", rocadaJson(trecho, equipe));

        mvc.perform(get("/api/intervencoes/trecho/{id}", trecho))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(put("/api/intervencoes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(rocadaJson(trecho, equipe).replace("4850.0", "5000.0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custo").value(5000.0));
        mvc.perform(delete("/api/intervencoes/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/intervencoes/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void intervencoesPorPeriodo() throws Exception {
        criar("/api/intervencoes", rocadaJson(criarTrecho(), criarEquipe("ROCADA")));

        mvc.perform(get("/api/intervencoes/periodo").param("inicio", hoje).param("fim", hoje))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // ------------------------------------------------------------- relatorios

    @Test
    void postDeRelatorioDevolve201ComAsFaixasContadas() throws Exception {
        criarTrecho(); // urgente
        criar("/api/trechos", trechoJson(80.0, 2.0)); // 10,8 cm: normal

        mvc.perform(post("/api/relatorios"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.qtUrgente").value(1))
                .andExpect(jsonPath("$.qtNormal").value(1))
                .andExpect(jsonPath("$.resumo").value(startsWith("1 de 2 trechos em estado urgente")));
    }

    @Test
    void historicoEPeriodoDeRelatorios() throws Exception {
        long id = criar("/api/relatorios", "");

        mvc.perform(get("/api/relatorios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/relatorios/periodo").param("inicio", hoje).param("fim", hoje))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/relatorios/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void periodoSemParametroOuInvertidoDevolve400() throws Exception {
        mvc.perform(get("/api/relatorios/periodo").param("inicio", hoje))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Parâmetro obrigatório ausente: fim"));
        mvc.perform(get("/api/relatorios/periodo").param("inicio", hoje).param("fim", "2020-01-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/relatorios/periodo").param("inicio", "ontem").param("fim", hoje))
                .andExpect(status().isBadRequest());
    }
}
