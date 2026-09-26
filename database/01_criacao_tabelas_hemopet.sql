-- HemoPet - Etapa 02
-- Entregavel 1: criacao do banco e das tabelas
-- SGBD: MySQL 8.0.16 ou superior (versoes anteriores nao aplicam CHECK)

CREATE DATABASE IF NOT EXISTS hemopet
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE hemopet;

CREATE TABLE pessoa (
    cpf CHAR(11) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    rua VARCHAR(100) NOT NULL,
    numero VARCHAR(10) NOT NULL,
    bairro VARCHAR(60) NOT NULL,
    cidade VARCHAR(60) NOT NULL,
    cep CHAR(8) NOT NULL,
    CONSTRAINT pk_pessoa PRIMARY KEY (cpf),
    CONSTRAINT ck_pessoa_cpf CHECK (cpf REGEXP '^[0-9]{11}$'),
    CONSTRAINT ck_pessoa_cep CHECK (cep REGEXP '^[0-9]{8}$')
) ENGINE = InnoDB;

CREATE TABLE telefone_pessoa (
    telefone VARCHAR(15) NOT NULL,
    cpf CHAR(11) NOT NULL,
    CONSTRAINT pk_telefone_pessoa PRIMARY KEY (telefone, cpf),
    CONSTRAINT ck_telefone_pessoa CHECK (telefone REGEXP '^[0-9]{10,15}$'),
    CONSTRAINT fk_telefone_pessoa_pessoa
        FOREIGN KEY (cpf) REFERENCES pessoa (cpf)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE tutor (
    cpf CHAR(11) NOT NULL,
    CONSTRAINT pk_tutor PRIMARY KEY (cpf),
    CONSTRAINT fk_tutor_pessoa
        FOREIGN KEY (cpf) REFERENCES pessoa (cpf)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE veterinario (
    cpf CHAR(11) NOT NULL,
    crmv VARCHAR(20) NOT NULL,
    cpf_supervisor CHAR(11) NULL,
    CONSTRAINT pk_veterinario PRIMARY KEY (cpf),
    CONSTRAINT uq_veterinario_crmv UNIQUE (crmv),
    CONSTRAINT fk_veterinario_pessoa
        FOREIGN KEY (cpf) REFERENCES pessoa (cpf)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_veterinario_supervisor
        FOREIGN KEY (cpf_supervisor) REFERENCES veterinario (cpf)
        ON UPDATE CASCADE
        ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE depende_tutor (
    id_dependente INT NOT NULL,
    cpf_tutor CHAR(11) NOT NULL,
    nome_contato_secundario VARCHAR(100) NOT NULL,
    parentesco VARCHAR(30) NOT NULL,
    CONSTRAINT pk_depende_tutor PRIMARY KEY (id_dependente, cpf_tutor),
    CONSTRAINT ck_depende_tutor_id CHECK (id_dependente > 0),
    CONSTRAINT fk_depende_tutor_tutor
        FOREIGN KEY (cpf_tutor) REFERENCES tutor (cpf)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE animal_doador (
    id_animal INT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(60) NOT NULL,
    especie VARCHAR(30) NOT NULL,
    raca VARCHAR(60) NOT NULL,
    tipo_sanguineo VARCHAR(20) NOT NULL,
    peso DECIMAL(5,2) NOT NULL,
    data_nascimento DATE NOT NULL,
    autorizacao_doacao BOOLEAN NOT NULL DEFAULT FALSE,
    cpf_tutor CHAR(11) NOT NULL,
    CONSTRAINT pk_animal_doador PRIMARY KEY (id_animal),
    CONSTRAINT ck_animal_especie CHECK (especie IN ('CAO', 'GATO')),
    CONSTRAINT ck_animal_peso CHECK (peso > 0),
    CONSTRAINT ck_animal_nascimento CHECK (data_nascimento >= '1990-01-01'),
    CONSTRAINT fk_animal_doador_tutor
        FOREIGN KEY (cpf_tutor) REFERENCES tutor (cpf)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE coleta (
    id_coleta INT NOT NULL AUTO_INCREMENT,
    data_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    volume_ml_total INT NOT NULL,
    status_aprovacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    id_animal INT NOT NULL,
    cpf_veterinario CHAR(11) NOT NULL,
    CONSTRAINT pk_coleta PRIMARY KEY (id_coleta),
    CONSTRAINT ck_coleta_volume CHECK (volume_ml_total BETWEEN 10 AND 1000),
    CONSTRAINT ck_coleta_status CHECK (
        status_aprovacao IN ('PENDENTE', 'APROVADA', 'REPROVADA')
    ),
    CONSTRAINT fk_coleta_animal
        FOREIGN KEY (id_animal) REFERENCES animal_doador (id_animal)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_coleta_veterinario
        FOREIGN KEY (cpf_veterinario) REFERENCES veterinario (cpf)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE bolsa_hemocomponente (
    id_bolsa INT NOT NULL AUTO_INCREMENT,
    tipo_componente VARCHAR(40) NOT NULL,
    data_fabricacao DATE NOT NULL,
    data_validade DATE NOT NULL,
    status_bolsa VARCHAR(20) NOT NULL DEFAULT 'EM_ESTOQUE',
    id_coleta INT NOT NULL,
    CONSTRAINT pk_bolsa_hemocomponente PRIMARY KEY (id_bolsa),
    CONSTRAINT ck_bolsa_tipo CHECK (
        tipo_componente IN (
            'SANGUE_TOTAL',
            'CONCENTRADO_HEMACIAS',
            'PLASMA_FRESCO_CONGELADO',
            'PLAQUETAS'
        )
    ),
    CONSTRAINT ck_bolsa_datas CHECK (data_validade > data_fabricacao),
    CONSTRAINT ck_bolsa_status CHECK (
        status_bolsa IN ('EM_ESTOQUE', 'RESERVADA', 'DISTRIBUIDA', 'DESCARTADA')
    ),
    CONSTRAINT fk_bolsa_coleta
        FOREIGN KEY (id_coleta) REFERENCES coleta (id_coleta)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE hospital_veterinario (
    id_hospital INT NOT NULL AUTO_INCREMENT,
    cnpj CHAR(14) NOT NULL,
    nome_fantasia VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_hospital_veterinario PRIMARY KEY (id_hospital),
    CONSTRAINT uq_hospital_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_hospital_cnpj CHECK (cnpj REGEXP '^[0-9]{14}$')
) ENGINE = InnoDB;

CREATE TABLE telefone_hospital (
    telefone VARCHAR(15) NOT NULL,
    id_hospital INT NOT NULL,
    CONSTRAINT pk_telefone_hospital PRIMARY KEY (telefone, id_hospital),
    CONSTRAINT ck_telefone_hospital CHECK (telefone REGEXP '^[0-9]{10,15}$'),
    CONSTRAINT fk_telefone_hospital_hospital
        FOREIGN KEY (id_hospital) REFERENCES hospital_veterinario (id_hospital)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE solicitacao (
    id_solicitacao INT NOT NULL AUTO_INCREMENT,
    data_solicitacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    urgencia VARCHAR(10) NOT NULL DEFAULT 'MEDIA',
    status_solicitacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    id_hospital INT NOT NULL,
    CONSTRAINT pk_solicitacao PRIMARY KEY (id_solicitacao),
    CONSTRAINT ck_solicitacao_urgencia CHECK (
        urgencia IN ('ALTA', 'MEDIA', 'BAIXA')
    ),
    CONSTRAINT ck_solicitacao_status CHECK (
        status_solicitacao IN ('PENDENTE', 'ATENDIDA', 'CANCELADA')
    ),
    CONSTRAINT fk_solicitacao_hospital
        FOREIGN KEY (id_hospital) REFERENCES hospital_veterinario (id_hospital)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE item_solicitacao (
    id_item INT NOT NULL AUTO_INCREMENT,
    quantidade INT NOT NULL DEFAULT 1,
    ht_percentual DECIMAL(4,1) NOT NULL,
    id_solicitacao INT NOT NULL,
    id_bolsa INT NOT NULL,
    CONSTRAINT pk_item_solicitacao PRIMARY KEY (id_item),
    CONSTRAINT uq_item_solicitacao_bolsa UNIQUE (id_solicitacao, id_bolsa),
    CONSTRAINT ck_item_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_item_ht CHECK (ht_percentual BETWEEN 0.0 AND 100.0),
    CONSTRAINT fk_item_solicitacao
        FOREIGN KEY (id_solicitacao) REFERENCES solicitacao (id_solicitacao)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_item_bolsa
        FOREIGN KEY (id_bolsa) REFERENCES bolsa_hemocomponente (id_bolsa)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE = InnoDB;

-- Indices adicionais para consultas frequentes por chaves estrangeiras.
CREATE INDEX idx_animal_cpf_tutor
    ON animal_doador (cpf_tutor);

CREATE INDEX idx_coleta_animal_data
    ON coleta (id_animal, data_hora);

CREATE INDEX idx_bolsa_status_validade
    ON bolsa_hemocomponente (status_bolsa, data_validade);

CREATE INDEX idx_solicitacao_hospital_status
    ON solicitacao (id_hospital, status_solicitacao);
