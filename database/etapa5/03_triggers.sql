-- HemoPet - Etapa 05
-- Entregavel 3: triggers
-- SGBD: MySQL 8.0.16 ou superior
-- Rodar DEPOIS de Criar_tabelas.sql e Insercao.sql

USE hemopet;

-- ---------------------------------------------------------------------
-- Tabela de logs usada pelo trigger 1.
-- Guarda o historico de mudancas de status das bolsas (rastreabilidade
-- exigida em bancos de sangue: quem mudou, de que para que, e quando).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS log_bolsa (
    id_log INT NOT NULL AUTO_INCREMENT,
    id_bolsa INT NOT NULL,
    status_anterior VARCHAR(20) NOT NULL,
    status_novo VARCHAR(20) NOT NULL,
    data_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario VARCHAR(100) NOT NULL,
    CONSTRAINT pk_log_bolsa PRIMARY KEY (id_log),
    CONSTRAINT fk_log_bolsa_bolsa
        FOREIGN KEY (id_bolsa) REFERENCES bolsa_hemocomponente (id_bolsa)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE = InnoDB;

DELIMITER $$

-- ---------------------------------------------------------------------
-- TRIGGER 1: trg_log_status_bolsa  (ATUALIZA A TABELA DE LOGS)
-- Justificativa semantica: toda vez que o status de uma bolsa muda
-- (EM_ESTOQUE -> RESERVADA -> DISTRIBUIDA / DESCARTADA), o hemocentro
-- precisa de um registro automatico para auditoria. Fazer isso por
-- trigger garante que o log exista mesmo se a mudanca vier de um
-- procedimento, da aplicacao ou de um UPDATE manual.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_log_status_bolsa$$

CREATE TRIGGER trg_log_status_bolsa
AFTER UPDATE ON bolsa_hemocomponente
FOR EACH ROW
BEGIN
    IF OLD.status_bolsa <> NEW.status_bolsa THEN
        INSERT INTO log_bolsa (id_bolsa, status_anterior, status_novo, usuario)
        VALUES (NEW.id_bolsa, OLD.status_bolsa, NEW.status_bolsa, CURRENT_USER());
    END IF;
END$$

-- ---------------------------------------------------------------------
-- TRIGGER 2: trg_bloqueia_coleta_sem_autorizacao
-- Justificativa semantica: so se pode coletar sangue de um animal cujo
-- tutor autorizou a doacao (campo autorizacao_doacao). A regra e de
-- negocio e de etica veterinaria, entao e barrada no proprio banco,
-- antes de a coleta ser gravada.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_bloqueia_coleta_sem_autorizacao$$

CREATE TRIGGER trg_bloqueia_coleta_sem_autorizacao
BEFORE INSERT ON coleta
FOR EACH ROW
BEGIN
    DECLARE v_autorizado BOOLEAN DEFAULT NULL;

    SELECT autorizacao_doacao
      INTO v_autorizado
      FROM animal_doador
     WHERE id_animal = NEW.id_animal;

    IF v_autorizado IS NULL OR v_autorizado = FALSE THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Coleta bloqueada: o tutor nao autorizou a doacao deste animal.';
    END IF;
END$$

DELIMITER ;
