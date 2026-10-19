-- Copia do seu-script-criacao.sql da Sprint 3, sem os DROP e o COMMIT.
-- Manter igual ao script: e contra estas tabelas que o validate confere as Entities.

CREATE TABLE equipe_manutencao (
    id               NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome             VARCHAR2(80)  NOT NULL,
    tipo             VARCHAR2(20)  NOT NULL,
    qt_integrantes   NUMBER(3)     NOT NULL,
    disponivel       NUMBER(1)     DEFAULT 1 NOT NULL,

    CONSTRAINT ck_equipe_disponivel  CHECK (disponivel IN (0, 1)),
    CONSTRAINT ck_equipe_tipo        CHECK (tipo IN ('ROCADA', 'PULVERIZACAO', 'MISTA')),
    CONSTRAINT ck_equipe_integrantes CHECK (qt_integrantes > 0)
);

CREATE TABLE trecho_rodovia (
    id                       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rodovia                  VARCHAR2(20)  NOT NULL,
    km_inicial               NUMBER(7,2)   NOT NULL,
    km_final                 NUMBER(7,2)   NOT NULL,
    especie_predominante     VARCHAR2(20)  NOT NULL,
    altura_vegetacao_cm      NUMBER(6,2)   NOT NULL,
    data_ultima_medicao      DATE          NOT NULL,
    data_ultima_intervencao  DATE,

    CONSTRAINT ck_trecho_km      CHECK (km_final > km_inicial),
    CONSTRAINT ck_trecho_altura  CHECK (altura_vegetacao_cm >= 0),
    CONSTRAINT uk_trecho_faixa   UNIQUE (rodovia, km_inicial, km_final),
    CONSTRAINT ck_trecho_especie CHECK (especie_predominante IN
        ('BRACHIARIA', 'CYNODON', 'MEGATHYRSUS', 'PENNISETUM', 'PASPALUM'))
);

CREATE TABLE intervencao_operacional (
    id                NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trecho_id         NUMBER        NOT NULL,
    equipe_id         NUMBER        NOT NULL,
    tipo              VARCHAR2(20)  NOT NULL,
    data_execucao     DATE          NOT NULL,
    custo             NUMBER(10,2)  NOT NULL,
    largura_faixa_m   NUMBER(5,2),
    altura_corte_cm   NUMBER(5,2),

    CONSTRAINT fk_interv_trecho FOREIGN KEY (trecho_id)
        REFERENCES trecho_rodovia (id),
    CONSTRAINT fk_interv_equipe FOREIGN KEY (equipe_id)
        REFERENCES equipe_manutencao (id),
    CONSTRAINT ck_interv_tipo  CHECK (tipo IN ('ROCADA', 'PULVERIZACAO')),
    CONSTRAINT ck_interv_custo CHECK (custo >= 0),
    CONSTRAINT ck_interv_subtipo CHECK (
        (tipo = 'ROCADA'
             AND largura_faixa_m IS NOT NULL AND altura_corte_cm IS NOT NULL)
        OR
        (tipo = 'PULVERIZACAO'
             AND largura_faixa_m IS NULL     AND altura_corte_cm IS NULL)
    )
);

CREATE INDEX ix_interv_trecho_data ON intervencao_operacional (trecho_id, data_execucao);

CREATE TABLE relatorio_prioridade (
    id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    data_geracao  TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    qt_urgente    NUMBER(5)      NOT NULL,
    qt_critico    NUMBER(5)      NOT NULL,
    qt_atencao    NUMBER(5)      NOT NULL,
    qt_normal     NUMBER(5)      NOT NULL,
    resumo        VARCHAR2(500),

    CONSTRAINT ck_relatorio_qt CHECK (
        qt_urgente >= 0 AND qt_critico >= 0 AND qt_atencao >= 0 AND qt_normal >= 0
    )
);
