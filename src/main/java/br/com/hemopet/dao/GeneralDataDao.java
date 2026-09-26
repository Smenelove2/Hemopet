package br.com.hemopet.dao;

import br.com.hemopet.config.DatabaseConfig;

import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class GeneralDataDao {
    private static final Map<String, String> TABLE_QUERIES = new LinkedHashMap<>();

    static {
        TABLE_QUERIES.put("Coletas", """
                SELECT c.id_coleta, c.data_hora, c.volume_ml_total, c.status_aprovacao,
                       a.nome AS animal, p.nome AS veterinario
                FROM coleta c
                JOIN animal_doador a ON a.id_animal = c.id_animal
                JOIN pessoa p ON p.cpf = c.cpf_veterinario
                ORDER BY c.id_coleta
                """);
        TABLE_QUERIES.put("Bolsas", """
                SELECT id_bolsa, tipo_componente, data_fabricacao, data_validade,
                       status_bolsa, id_coleta
                FROM bolsa_hemocomponente
                ORDER BY id_bolsa
                """);
        TABLE_QUERIES.put("Solicitacoes", """
                SELECT s.id_solicitacao, s.data_solicitacao, s.urgencia,
                       s.status_solicitacao, h.nome_fantasia AS hospital
                FROM solicitacao s
                JOIN hospital_veterinario h ON h.id_hospital = s.id_hospital
                ORDER BY s.id_solicitacao
                """);
        TABLE_QUERIES.put("Itens de solicitacao", """
                SELECT id_item, quantidade, ht_percentual, id_solicitacao, id_bolsa
                FROM item_solicitacao
                ORDER BY id_item
                """);
    }

    public Map<String, String> getViews() {
        return TABLE_QUERIES;
    }

    public DefaultTableModel list(String viewName) {
        String sql = TABLE_QUERIES.get(viewName);
        if (sql == null) {
            throw new IllegalArgumentException("Visualizacao nao encontrada: " + viewName);
        }

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return ResultSetTableMapper.toTableModel(resultSet);
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel carregar dados.", exception);
        }
    }
}
