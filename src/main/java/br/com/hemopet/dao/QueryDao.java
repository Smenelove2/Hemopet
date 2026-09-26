package br.com.hemopet.dao;

import br.com.hemopet.config.DatabaseConfig;

import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class QueryDao {
    private static final Map<String, String> QUERIES = new LinkedHashMap<>();

    static {
        QUERIES.put("Animais por especie", """
                SELECT especie, COUNT(*) AS quantidade_animais_por_especie
                FROM animal_doador
                GROUP BY especie
                ORDER BY especie ASC
                """);
        QUERIES.put("Animais e tutores", """
                SELECT p.nome AS tutor, a.nome AS animal
                FROM pessoa p
                JOIN tutor t ON p.cpf = t.cpf
                JOIN animal_doador a ON a.cpf_tutor = t.cpf
                ORDER BY p.nome ASC, a.nome ASC
                """);
        QUERIES.put("Animais acima da media de peso", """
                SELECT a.nome AS animal, p.nome AS tutor, a.peso
                FROM pessoa p
                JOIN tutor t ON p.cpf = t.cpf
                JOIN animal_doador a ON a.cpf_tutor = t.cpf
                WHERE a.peso > (
                    SELECT AVG(peso)
                    FROM animal_doador
                )
                ORDER BY a.peso DESC
                """);
        QUERIES.put("Tutores com cao e gato", """
                SELECT p.nome AS tutor,
                       SUM(a.especie = 'CAO') AS quantidade_caes,
                       SUM(a.especie = 'GATO') AS quantidade_gatos
                FROM pessoa p
                JOIN tutor t ON p.cpf = t.cpf
                JOIN animal_doador a ON a.cpf_tutor = t.cpf
                GROUP BY p.cpf, p.nome
                HAVING SUM(a.especie = 'CAO') > 0
                   AND SUM(a.especie = 'GATO') > 0
                ORDER BY p.nome ASC
                """);
    }

    public Map<String, String> getQueryNames() {
        return QUERIES;
    }

    public DefaultTableModel execute(String name) {
        String sql = QUERIES.get(name);
        if (sql == null) {
            throw new IllegalArgumentException("Consulta nao encontrada: " + name);
        }

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return ResultSetTableMapper.toTableModel(resultSet);
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel executar consulta.", exception);
        }
    }
}
