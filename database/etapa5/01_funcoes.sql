-- HemoPet - Etapa 05
-- Entregavel 1: funcoes
-- SGBD: MySQL 8.0.16 ou superior
-- Rodar DEPOIS de Criar_tabelas.sql e Insercao.sql

USE hemopet;

DELIMITER $$

-- ---------------------------------------------------------------------
-- FUNCAO 1: fn_idade_animal
-- Justificativa semantica: a elegibilidade de um animal para doar sangue
-- depende da idade. Em vez de repetir TIMESTAMPDIFF em varias consultas,
-- a regra de calculo fica centralizada em um unico lugar.
-- Retorna a idade em anos completos, ou NULL se o animal nao existir.
-- ---------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_idade_animal$$

CREATE FUNCTION fn_idade_animal(p_id_animal INT)
RETURNS INT
READS SQL DATA
BEGIN
    DECLARE v_nascimento DATE DEFAULT NULL;

    SELECT data_nascimento
      INTO v_nascimento
      FROM animal_doador
     WHERE id_animal = p_id_animal;

    IF v_nascimento IS NULL THEN
        RETURN NULL;
    END IF;

    RETURN TIMESTAMPDIFF(YEAR, v_nascimento, CURDATE());
END$$

-- ---------------------------------------------------------------------
-- FUNCAO 2: fn_status_validade (USA ESTRUTURA CONDICIONAL: IF / ELSEIF)
-- Justificativa semantica: hemocomponentes sao perecíveis (plaquetas
-- duram poucos dias, plasma congelado dura um ano). O hemocentro precisa
-- saber rapidamente se uma bolsa pode ser entregue a um hospital.
-- Retorna: 'INEXISTENTE', 'VENCIDA', 'VENCE_EM_7_DIAS' ou 'VALIDA'.
-- ---------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_status_validade$$

CREATE FUNCTION fn_status_validade(p_id_bolsa INT)
RETURNS VARCHAR(20)
READS SQL DATA
BEGIN
    DECLARE v_validade DATE DEFAULT NULL;
    DECLARE v_dias INT;
    DECLARE v_situacao VARCHAR(20);

    SELECT data_validade
      INTO v_validade
      FROM bolsa_hemocomponente
     WHERE id_bolsa = p_id_bolsa;

    IF v_validade IS NULL THEN
        SET v_situacao = 'INEXISTENTE';
    ELSE
        SET v_dias = DATEDIFF(v_validade, CURDATE());

        IF v_dias < 0 THEN
            SET v_situacao = 'VENCIDA';
        ELSEIF v_dias <= 7 THEN
            SET v_situacao = 'VENCE_EM_7_DIAS';
        ELSE
            SET v_situacao = 'VALIDA';
        END IF;
    END IF;

    RETURN v_situacao;
END$$

DELIMITER ;
