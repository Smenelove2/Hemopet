-- HemoPet - Etapa 05
-- Entregavel 2: procedimentos
-- SGBD: MySQL 8.0.16 ou superior
-- Rodar DEPOIS de Criar_tabelas.sql e Insercao.sql

USE hemopet;

DELIMITER $$

-- ---------------------------------------------------------------------
-- PROCEDIMENTO 1 (ATUALIZACAO DE DADOS): sp_descartar_bolsas_vencidas
-- Justificativa semantica: bolsa vencida nao pode ser usada. Este
-- procedimento faz a rotina de limpeza do estoque: muda para DESCARTADA
-- todas as bolsas EM_ESTOQUE ou RESERVADA cuja validade ja passou.
-- Parametros:
--   p_data_ref : data de referencia (NULL = hoje)
--   p_total    : devolve quantas bolsas foram descartadas
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_descartar_bolsas_vencidas$$

CREATE PROCEDURE sp_descartar_bolsas_vencidas(
    IN p_data_ref DATE,
    OUT p_total INT
)
BEGIN
    DECLARE v_data DATE;

    SET v_data = IFNULL(p_data_ref, CURDATE());

    UPDATE bolsa_hemocomponente
       SET status_bolsa = 'DESCARTADA'
     WHERE status_bolsa IN ('EM_ESTOQUE', 'RESERVADA')
       AND data_validade < v_data;

    SET p_total = ROW_COUNT();
END$$

-- ---------------------------------------------------------------------
-- PROCEDIMENTO 2 (COM CURSOR): sp_processar_solicitacao
-- Justificativa semantica: ao atender a solicitacao de um hospital, cada
-- bolsa pedida precisa ser avaliada INDIVIDUALMENTE e cada uma pode ter
-- um destino diferente:
--   - bolsa valida            -> vira DISTRIBUIDA
--   - bolsa ja distribuida    -> conta como ja entregue
--   - bolsa vencida           -> e DESCARTADA e vira um problema
--   - bolsa descartada antes  -> vira um problema
-- Um unico UPDATE aplicaria a mesma regra a todas as linhas; aqui a
-- decisao muda a cada linha (e ainda gera um relatorio de problemas),
-- por isso o cursor e necessario.
-- No fim: se nao houve nenhum problema, a solicitacao vira ATENDIDA;
-- se houve, continua PENDENTE (o hospital precisa de outra bolsa).
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_processar_solicitacao$$

CREATE PROCEDURE sp_processar_solicitacao(IN p_id_solicitacao INT)
BEGIN
    DECLARE v_fim TINYINT DEFAULT 0;
    DECLARE v_existe INT DEFAULT 0;
    DECLARE v_status_solicitacao VARCHAR(20);
    DECLARE v_id_item INT;
    DECLARE v_id_bolsa INT;
    DECLARE v_status_bolsa VARCHAR(20);
    DECLARE v_validade DATE;
    DECLARE v_distribuidas INT DEFAULT 0;
    DECLARE v_ja_entregues INT DEFAULT 0;
    DECLARE v_problemas INT DEFAULT 0;

    DECLARE cur_itens CURSOR FOR
        SELECT id_item, id_bolsa
          FROM item_solicitacao
         WHERE id_solicitacao = p_id_solicitacao
         ORDER BY id_item;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_fim = 1;

    -- Validacoes iniciais
    SELECT COUNT(*) INTO v_existe
      FROM solicitacao
     WHERE id_solicitacao = p_id_solicitacao;

    IF v_existe = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solicitacao nao encontrada.';
    END IF;

    SELECT status_solicitacao INTO v_status_solicitacao
      FROM solicitacao
     WHERE id_solicitacao = p_id_solicitacao;

    IF v_status_solicitacao <> 'PENDENTE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Somente solicitacoes PENDENTES podem ser processadas.';
    END IF;

    -- Relatorio de problemas desta execucao
    DROP TEMPORARY TABLE IF EXISTS tmp_problemas_solicitacao;
    CREATE TEMPORARY TABLE tmp_problemas_solicitacao (
        id_item INT,
        id_bolsa INT,
        motivo VARCHAR(60)
    );

    OPEN cur_itens;

    leitura: LOOP
        FETCH cur_itens INTO v_id_item, v_id_bolsa;

        IF v_fim = 1 THEN
            LEAVE leitura;
        END IF;

        SELECT status_bolsa, data_validade
          INTO v_status_bolsa, v_validade
          FROM bolsa_hemocomponente
         WHERE id_bolsa = v_id_bolsa;

        IF v_status_bolsa = 'DISTRIBUIDA' THEN
            SET v_ja_entregues = v_ja_entregues + 1;

        ELSEIF v_status_bolsa = 'DESCARTADA' THEN
            SET v_problemas = v_problemas + 1;
            INSERT INTO tmp_problemas_solicitacao
            VALUES (v_id_item, v_id_bolsa, 'BOLSA JA DESCARTADA');

        ELSEIF v_validade < CURDATE() THEN
            UPDATE bolsa_hemocomponente
               SET status_bolsa = 'DESCARTADA'
             WHERE id_bolsa = v_id_bolsa;
            SET v_problemas = v_problemas + 1;
            INSERT INTO tmp_problemas_solicitacao
            VALUES (v_id_item, v_id_bolsa, 'BOLSA VENCIDA (DESCARTADA)');

        ELSE
            UPDATE bolsa_hemocomponente
               SET status_bolsa = 'DISTRIBUIDA'
             WHERE id_bolsa = v_id_bolsa;
            SET v_distribuidas = v_distribuidas + 1;
        END IF;
    END LOOP leitura;

    CLOSE cur_itens;

    IF v_problemas = 0 THEN
        UPDATE solicitacao
           SET status_solicitacao = 'ATENDIDA'
         WHERE id_solicitacao = p_id_solicitacao;
    END IF;

    -- Resultado 1: resumo
    SELECT p_id_solicitacao AS id_solicitacao,
           v_distribuidas   AS bolsas_distribuidas_agora,
           v_ja_entregues   AS bolsas_ja_entregues,
           v_problemas      AS itens_com_problema,
           (SELECT status_solicitacao
              FROM solicitacao
             WHERE id_solicitacao = p_id_solicitacao) AS status_final;

    -- Resultado 2: detalhes dos problemas (vazio se nao houve)
    SELECT * FROM tmp_problemas_solicitacao;

    DROP TEMPORARY TABLE IF EXISTS tmp_problemas_solicitacao;
END$$

DELIMITER ;
