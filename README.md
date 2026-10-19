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
| `DELETE` | `/api/{recurso}/{id}` | `204` | `404` |

### Relatório

| Método | Rota | O que faz |
| --- | --- | --- |
| `POST` | `/api/relatorios` | gera o relatório de prioridade e persiste |
| `GET` | `/api/relatorios` | histórico de relatórios |
| `GET` | `/api/relatorios/periodo?inicio=AAAA-MM-DD&fim=AAAA-MM-DD` | relatórios gerados no período |

### Derived queries

O Spring Data monta o SQL de cada método a partir do nome dele:

| Repository | Método |
| --- | --- |
| `TrechoRodoviaRepository` | _(a preencher)_ |
| `EquipeManutencaoRepository` | _(a preencher)_ |
| `IntervencaoOperacionalRepository` | _(a preencher)_ |
| `RelatorioPrioridadeRepository` | _(a preencher)_ |

---

## 6. Exemplos com cURL

```bash
# cria um trecho
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

# lista e busca
curl -i http://localhost:8080/api/trechos
curl -i http://localhost:8080/api/trechos/1

# atualiza
curl -i -X PUT http://localhost:8080/api/trechos/1 \
  -H "Content-Type: application/json" \
  -d '{ "rodovia": "SP-280", "kmInicial": 12.0, "kmFinal": 14.5,
        "especiePredominante": "BRACHIARIA", "alturaVegetacaoCm": 27.0,
        "dataUltimaMedicao": "2026-10-14" }'

# apaga
curl -i -X DELETE http://localhost:8080/api/trechos/1

# gera um relatório e consulta o histórico
curl -i -X POST http://localhost:8080/api/relatorios
curl -i http://localhost:8080/api/relatorios
curl -i "http://localhost:8080/api/relatorios/periodo?inicio=2026-10-01&fim=2026-10-31"
```

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
