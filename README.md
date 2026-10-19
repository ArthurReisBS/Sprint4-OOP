# MOTIVA — API REST com Spring Boot + JPA

Sistema de priorização de roçada em rodovias. Na Sprint 3 ele era uma aplicação de console
com **JDBC puro** e **DAO**; aqui vira uma **API REST** com Spring Boot e Spring Data JPA,
usando **as mesmas tabelas Oracle da Sprint 3**.

Requer **Java 17** e Maven.

## Integrantes

| Integrante | RM |
| --- | --- |
| Arthur | RM562181 |
| Isabelle | RM566464 |
| Carol | RM564651 |
| Leonardo | RM563663 |
| Manoella | RM564469 |
| Júlia | RM565010 |

---

## 1. Como rodar

### 1.1 Credenciais

Elas vêm de variáveis de ambiente e nunca ficam no código nem no `application.properties`.

```bash
export DB_USER=seu_rm_da_fiap
export DB_PASSWORD=sua_senha
```

O `application.properties` só referencia as variáveis:

```properties
spring.datasource.url=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
```

### 1.2 Tabelas

As tabelas são as da Sprint 3, então o Hibernate não cria nem altera o schema. Com
ddl-auto=validate ele só confere se as Entities batem com as tabelas, e se alguma coluna
não bater a aplicação não sobe.

Se as tabelas ainda não existirem no seu usuário, rode os dois scripts da Sprint 3 com a
ferramenta ExecutarScript de lá, que usa as mesmas variáveis de ambiente. Dentro da pasta
da Sprint 3:

```bash
java -cp "bin:lib/ojdbc17.jar" br.com.motiva.ferramentas.ExecutarScript sql/seu-script-criacao.sql
java -cp "bin:lib/ojdbc17.jar" br.com.motiva.ferramentas.ExecutarScript sql/seu-script-dados.sql
```

Na primeira execução os quatro DROP TABLE do começo do script de criação falham, porque
ainda não há tabela para apagar. É esperado e a ferramenta segue para os CREATE. Também
dá para abrir os dois arquivos no SQL Developer e rodar como script.

O script de dados cria 4 equipes, 9 trechos, 9 intervenções e 1 relatório. As datas são
relativas ao dia em que ele roda, então os trechos caem sempre nas mesmas faixas de
prioridade.

### 1.3 Executar

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

### 1.4 Testes

```bash
./mvnw test
```

Os testes não precisam do Oracle. O perfil test, em src/test/resources, troca o banco por
um H2 em memória no modo Oracle, criado a partir de uma cópia do script da Sprint 3
(schema-test.sql). Assim os testes esbarram nas mesmas CHECK, chaves estrangeiras, UNIQUE
e IDENTITY do banco real.

| Pacote | O que é testado |
| --- | --- |
| model | as regras simples das entidades e dos enums, como qual equipe pode executar qual intervenção |
| service | as validações e o motor de prioridade, com os Repositories simulados pelo Mockito; o motor é conferido com os 9 trechos do script de dados da Sprint 3 |
| repository | cada derived query e as constraints do banco, contra o H2 |
| controller | a API inteira com @SpringBootTest e MockMvc, do HTTP até o banco: o status de cada endpoint e o corpo das respostas de erro |

O H2 não reconhece todos os tipos de coluna do Oracle, então no perfil test o ddl-auto
fica em none. A conferência das Entities contra as tabelas acontece quando a aplicação
sobe no Oracle.

---

## 2. Arquitetura

```
src/main/java/br/com/motiva/
├── MotivaApplication.java   @SpringBootApplication
├── model/                   @Entity: as entidades da Sprint 3
├── repository/              interfaces JpaRepository, sem SQL
├── service/                 @Service: validações + motor de prioridade
└── controller/              @RestController: só HTTP
```

```
Controller  →  Service  →  Repository  →  Oracle
```

Cada camada só conversa com a vizinha. O Controller recebe a requisição e devolve o
status HTTP, o Service decide (valida e classifica) e o Repository persiste.
Regra de negócio não entra no Controller, e SQL escrito à mão não entra em lugar nenhum.

---

## 3. Modelo

| Entidade | Tabela |
| --- | --- |
| `EquipeManutencao` | `EQUIPE_MANUTENCAO` |
| `TrechoRodovia` | `TRECHO_RODOVIA` |
| `IntervencaoOperacional` → `RocadaMecanizada`, `Pulverizacao` | `INTERVENCAO_OPERACIONAL` |
| `RelatorioPrioridade` | `RELATORIO_PRIORIDADE` |

**De `record` para classe.** Na Sprint 3 a intervenção era uma interface `sealed` com
dois `record`s. O JPA precisa de classe com construtor vazio e campos mutáveis, então a
hierarquia virou uma classe abstrata com `@Inheritance(strategy = SINGLE_TABLE)` e
`@DiscriminatorColumn(name = "TIPO")`. A tabela é a mesma: as duas subclasses continuam
dividindo `INTERVENCAO_OPERACIONAL`, separadas pela coluna `TIPO`.

**Geração de ID.** O enunciado pede `@SequenceGenerator`, mas as tabelas da Sprint 3 foram
criadas com `GENERATED ALWAYS AS IDENTITY` e não têm sequence. Como esta sprint reaproveita
essas tabelas sem alterá-las (`ddl-auto=validate`), as Entities usam
`@GeneratedValue(strategy = GenerationType.IDENTITY)`: o Oracle continua gerando o ID, como
já fazia na Sprint 3, e o Hibernate só lê o valor depois do `INSERT`. Usar sequence exigiria
criar as sequences e tirar o `IDENTITY` das 4 tabelas, ou seja, mudar um schema que já
funciona só para trocar quem gera o número.

---

## 4. Regra de priorização

A classificação mora no `@Service`, nunca no Controller. A prioridade **não é
persistida**: ela é calculada em tempo de execução porque muda com o tempo, mesmo que
ninguém mexa no trecho.

```
altura estimada = altura medida + taxa da espécie × dias desde a medição,
                  limitada ao porte máximo da espécie
```

| Prioridade | Altura estimada |
| --- | --- |
| `NORMAL` | ≤ 15 cm |
| `ATENCAO` | 16 a 25 cm |
| `CRITICO` | 26 a 29 cm |
| `URGENTE` | ≥ 30 cm |

As validações da Sprint 1 também ficam no Service, como `nivelVegetacao >= 0`.

---

## 5. Endpoints

### CRUD

Mesmo padrão para `/api/trechos`, `/api/equipes` e `/api/intervencoes`:

| Método | Rota | Sucesso | Erro |
| --- | --- | --- | --- |
| `GET` | `/api/{recurso}` | `200` lista | — |
| `GET` | `/api/{recurso}/{id}` | `200` | `404` |
| `POST` | `/api/{recurso}` | `201` + corpo criado | `400` |
| `PUT` | `/api/{recurso}/{id}` | `200` | `400` · `404` |
| `DELETE` | `/api/{recurso}/{id}` | `204` | `400` · `404` |

O DELETE de um trecho ou de uma equipe devolve 400 quando há intervenções no histórico,
porque a chave estrangeira da tabela de intervenções impediria a exclusão.

### Consultas

| Método | Rota | O que devolve |
| --- | --- | --- |
| GET | /api/trechos?rodovia=SP-021 | trechos da rodovia, em ordem de km |
| GET | /api/trechos/prioridade/{prioridade} | trechos de uma faixa (URGENTE, CRITICO, ATENCAO ou NORMAL) pela altura estimada de hoje |
| GET | /api/equipes?tipo=ROCADA | equipes de um tipo (ROCADA, PULVERIZACAO ou MISTA) |
| GET | /api/equipes/disponiveis | equipes disponíveis |
| GET | /api/intervencoes/trecho/{id} | histórico do trecho, em ordem de data |
| GET | /api/intervencoes/equipe/{id} | intervenções feitas pela equipe |
| GET | /api/intervencoes/periodo?inicio=AAAA-MM-DD&fim=AAAA-MM-DD | intervenções executadas no período |
| GET | /api/relatorios/{id} | um relatório do histórico |

### Corpo da intervenção

Trechos e equipes entram e saem da API no formato da própria entidade. A intervenção
usa DTOs, porque IntervencaoOperacional é uma classe abstrata e o JSON precisa dizer qual
das duas subclasses criar. Quem decide é o campo tipo, e o trecho e a equipe chegam só
pelo id:

```json
{
  "tipo": "ROCADA",
  "trechoId": 1,
  "equipeId": 1,
  "dataExecucao": "2026-10-19",
  "custo": 4850.0,
  "larguraFaixaM": 3.5,
  "alturaCorteCm": 10.0
}
```

Uma pulverização leva os mesmos campos, menos larguraFaixaM e alturaCorteCm. Se eles
vierem preenchidos, a resposta é 400. A equipe também precisa saber executar aquele tipo
de serviço: uma equipe de pulverização não registra roçada, enquanto uma equipe mista
registra as duas.

Na resposta, o trecho e a equipe aparecem resumidos em id e nome, e não como objetos
inteiros:

```json
{
  "id": 10,
  "tipo": "ROCADA",
  "descricao": "Roçada de 3,5 m de faixa, corte a 10 cm",
  "trechoId": 1,
  "trecho": "SP-021 km 12,0-18,5",
  "equipeId": 1,
  "equipe": "Equipe Alfa - Roçada Norte",
  "dataExecucao": "2026-10-19",
  "custo": 4850.0,
  "larguraFaixaM": 3.5,
  "alturaCorteCm": 10.0
}
```

### Erros

Toda resposta de erro tem o mesmo formato, montado pelo GlobalExceptionHandler. Ele
transforma as exceções dos Services em status HTTP, para os Controllers não precisarem
de try/catch:

```json
{
  "timestamp": "2026-10-19T14:02:11",
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Trecho 9999 não encontrado(a).",
  "caminho": "/api/trechos/9999"
}
```

| Situação | Status |
| --- | --- |
| id que não existe | 404 |
| dado que fere uma regra (altura negativa, km final menor que o inicial, faixa repetida, equipe incompatível) | 400 |
| JSON mal formado, enum com valor inexistente, data fora do formato AAAA-MM-DD | 400 |
| id que não é número, parâmetro obrigatório faltando | 400 |

### Relatório

| Método | Rota | O que faz |
| --- | --- | --- |
| `POST` | `/api/relatorios` | gera o relatório de prioridade e persiste |
| `GET` | `/api/relatorios` | histórico de relatórios |
| `GET` | `/api/relatorios/periodo?inicio=AAAA-MM-DD&fim=AAAA-MM-DD` | relatórios gerados no período |

### Derived queries

Nenhum Repository tem SQL. O Spring Data monta a consulta de cada método a partir do
nome dele, e o Hibernate gera o SQL do Oracle. A coluna da direita mostra o filtro que
cada nome produz.

| Repository | Método | Filtro gerado |
| --- | --- | --- |
| TrechoRodoviaRepository | findByRodoviaOrderByKmInicialAsc | WHERE rodovia = ? ORDER BY km_inicial |
| | findByEspeciePredominante | WHERE especie_predominante = ? |
| | findByAlturaVegetacaoCmGreaterThanEqual | WHERE altura_vegetacao_cm >= ? |
| | findByDataUltimaIntervencaoIsNull | WHERE data_ultima_intervencao IS NULL |
| | existsByRodoviaAndKmInicialAndKmFinal | WHERE rodovia = ? AND km_inicial = ? AND km_final = ? |
| EquipeManutencaoRepository | findByTipo | WHERE tipo = ? |
| | findByDisponivelTrue | WHERE disponivel = 1 |
| | findByQuantidadeIntegrantesGreaterThanEqual | WHERE qt_integrantes >= ? |
| IntervencaoOperacionalRepository | findByTrechoIdOrderByDataExecucaoAsc | WHERE trecho_id = ? ORDER BY data_execucao |
| | findByEquipeId | WHERE equipe_id = ? |
| | findByDataExecucaoBetween | WHERE data_execucao BETWEEN ? AND ? |
| | findByCustoGreaterThanEqual | WHERE custo >= ? |
| | existsByTrechoId, existsByEquipeId | WHERE trecho_id = ? / WHERE equipe_id = ? |
| RelatorioPrioridadeRepository | findAllByOrderByDataGeracaoDesc | ORDER BY data_geracao DESC |
| | findByDataGeracaoBetweenOrderByDataGeracaoAsc | WHERE data_geracao BETWEEN ? AND ? ORDER BY data_geracao |
| | findByQtUrgenteGreaterThanEqual | WHERE qt_urgente >= ? |

Os métodos exists servem aos Services. Antes de inserir um trecho, o Service confere se a
faixa já existe; antes de apagar um trecho ou uma equipe, confere se há histórico. Assim
a API devolve uma mensagem clara em vez do erro de constraint do Oracle.

Os métodos básicos, como save, findById, findAll e deleteById, já vêm do JpaRepository.

---

## 6. Exemplos com cURL

Os exemplos partem dos dados do script da Sprint 3: os trechos e as equipes têm id de 1 a
9 e de 1 a 4, e o trecho criado no primeiro exemplo ganha o próximo id livre. Nos
exemplos ele é o 10; se for outro, troque pelo id que o POST devolver.

```bash
# cria um trecho (201)
curl -i -X POST http://localhost:8080/api/trechos \
  -H "Content-Type: application/json" \
  -d '{
        "rodovia": "SP-280",
        "kmInicial": 12.0,
        "kmFinal": 14.5,
        "especiePredominante": "BRACHIARIA",
        "alturaVegetacaoCm": 18.0,
        "dataUltimaMedicao": "2026-10-10"
      }'

# lista, busca e filtra (200)
curl -i http://localhost:8080/api/trechos
curl -i http://localhost:8080/api/trechos/10
curl -i "http://localhost:8080/api/trechos?rodovia=SP-021"
curl -i http://localhost:8080/api/trechos/prioridade/URGENTE

# atualiza o trecho criado (200)
curl -i -X PUT http://localhost:8080/api/trechos/10 \
  -H "Content-Type: application/json" \
  -d '{ "rodovia": "SP-280", "kmInicial": 12.0, "kmFinal": 14.5,
        "especiePredominante": "BRACHIARIA", "alturaVegetacaoCm": 27.0,
        "dataUltimaMedicao": "2026-10-14" }'

# apaga o trecho criado (204); ele não tem histórico
curl -i -X DELETE http://localhost:8080/api/trechos/10

# trecho que não existe (404)
curl -i http://localhost:8080/api/trechos/9999

# altura negativa (400)
curl -i -X POST http://localhost:8080/api/trechos \
  -H "Content-Type: application/json" \
  -d '{ "rodovia": "SP-280", "kmInicial": 20.0, "kmFinal": 22.0,
        "especiePredominante": "CYNODON", "alturaVegetacaoCm": -5.0,
        "dataUltimaMedicao": "2026-10-10" }'

# equipes de roçada (200)
curl -i "http://localhost:8080/api/equipes?tipo=ROCADA"

# registra uma roçada no trecho 1 pela Equipe Alfa, que é de roçada (201)
curl -i -X POST http://localhost:8080/api/intervencoes \
  -H "Content-Type: application/json" \
  -d '{ "tipo": "ROCADA", "trechoId": 1, "equipeId": 1,
        "dataExecucao": "2026-10-19", "custo": 4850.0,
        "larguraFaixaM": 3.5, "alturaCorteCm": 10.0 }'

# a mesma roçada pela Equipe Charlie, que só pulveriza (400)
curl -i -X POST http://localhost:8080/api/intervencoes \
  -H "Content-Type: application/json" \
  -d '{ "tipo": "ROCADA", "trechoId": 1, "equipeId": 3,
        "dataExecucao": "2026-10-19", "custo": 4850.0,
        "larguraFaixaM": 3.5, "alturaCorteCm": 10.0 }'

# histórico do trecho 1 (200)
curl -i http://localhost:8080/api/intervencoes/trecho/1

# o trecho 1 tem histórico, então não pode ser apagado (400)
curl -i -X DELETE http://localhost:8080/api/trechos/1

# gera um relatório e consulta o histórico (201 e 200)
curl -i -X POST http://localhost:8080/api/relatorios
curl -i http://localhost:8080/api/relatorios
curl -i "http://localhost:8080/api/relatorios/periodo?inicio=2026-10-01&fim=2026-10-31"
```

Com os dados do script, o relatório gerado traz 2 trechos urgentes, 2 críticos, 2 em
atenção e 3 normais, mais o que tiver sido criado pelos exemplos acima.

---

## 7. Requisições funcionando

_(a preencher: pelo menos 6 prints, cobrindo CRUD e relatório)_

| # | Requisição | Print |
| --- | --- | --- |
| 1 | `POST /api/trechos` → 201 | |
| 2 | `GET /api/trechos` → 200 | |
| 3 | `GET /api/trechos/{id}` inexistente → 404 | |
| 4 | `PUT /api/trechos/{id}` → 200 | |
| 5 | `DELETE /api/trechos/{id}` → 204 | |
| 6 | `POST /api/relatorios` → 201 | |

---

## 8. Perguntas para reflexão

**1. Por que o Repository é uma interface e não uma classe? Quem escreve a implementação e quando?**

_(a preencher)_

**2. O pattern DAO da Sprint 3 "morreu" na migração ou só mudou de forma?**

_(a preencher)_

**3. Por que a validação de `nivelVegetacao >= 0` (Sprint 1) deve ficar no Service e não no Controller?**

_(a preencher)_

**4. No JDBC puro escrevíamos SQL. Onde está o SQL do `findByTipo()`? Quem o gerou?**

_(a preencher)_
