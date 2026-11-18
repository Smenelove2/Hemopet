package br.com.hemopet.dao;

import br.com.hemopet.config.DatabaseConfig;
import br.com.hemopet.model.DashboardMetric;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardDao {
    public Map<String, Integer> countAnimalsBySpecies() {
        return countGrouped("""
                SELECT especie AS label, COUNT(*) AS total
                FROM animal_doador
                GROUP BY especie
                ORDER BY especie
                """);
    }

    public Map<String, Integer> countBagsByStatus() {
        return countGrouped("""
                SELECT status_bolsa AS label, COUNT(*) AS total
                FROM bolsa_hemocomponente
                GROUP BY status_bolsa
                ORDER BY status_bolsa
                """);
    }

    public Map<String, Integer> countRequestsByUrgency() {
        return countGrouped("""
                SELECT urgencia AS label, COUNT(*) AS total
                FROM solicitacao
                GROUP BY urgencia
                ORDER BY urgencia
                """);
    }

    public DashboardMetric countAnimals() {
        return countSingle("Animais", "SELECT COUNT(*) FROM animal_doador");
    }

    public DashboardMetric countHospitals() {
        return countSingle("Hospitais", "SELECT COUNT(*) FROM hospital_veterinario");
    }

    public DashboardMetric countPendingRequests() {
        return countSingle("Solicitacoes pendentes", "SELECT COUNT(*) FROM solicitacao WHERE status_solicitacao = 'PENDENTE'");
    }

    public DashboardMetric countAvailableBags() {
        return countSingle("Bolsas em estoque", "SELECT COUNT(*) FROM bolsa_hemocomponente WHERE status_bolsa = 'EM_ESTOQUE'");
    }

    public DashboardMetric countPeople() {
    return countSingle("Pessoas", "SELECT COUNT(*) FROM pessoa");
    }

    public DashboardMetric countVeterinarians() {
        return countSingle("Veterinarios", "SELECT COUNT(*) FROM veterinario");
    }

    public DashboardMetric countCollections() {
        return countSingle("Coletas", "SELECT COUNT(*) FROM coleta");
    }

    private Map<String, Integer> countGrouped(String sql) {
        Map<String, Integer> values = new LinkedHashMap<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                values.put(resultSet.getString("label"), resultSet.getInt("total"));
            }
            return values;
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel carregar dados do dashboard.", exception);
        }
    }

    public Map<String, Integer> countAnimalsByWeightRange() {
       return countGrouped("""
            SELECT faixa AS label, COUNT(*) AS total
            FROM (
                SELECT
                    CASE
                        WHEN peso < 5 THEN '0-5 kg'
                        WHEN peso < 10 THEN '5-10 kg'
                        WHEN peso < 20 THEN '10-20 kg'
                        WHEN peso < 30 THEN '20-30 kg'
                        ELSE '30+ kg'
                    END AS faixa,
                    CASE
                        WHEN peso < 5 THEN 1
                        WHEN peso < 10 THEN 2
                        WHEN peso < 20 THEN 3
                        WHEN peso < 30 THEN 4
                        ELSE 5
                    END AS ordem
                FROM animal_doador
                WHERE peso IS NOT NULL
            ) AS distribuicao
            GROUP BY faixa, ordem
            ORDER BY ordem
            """);
    }

    public Map<String, Integer> countCollectionsByApprovalStatus() {
       return countGrouped("""
                SELECT status_aprovacao AS label, COUNT(*) AS total
                FROM coleta
                GROUP BY status_aprovacao
                ORDER BY status_aprovacao
                """);
    }
    
    public Map<String, Double> sumCollectionVolumeByMonth() {
    String sql = """
            SELECT
                DATE_FORMAT(data_hora, '%Y-%m') AS label,
                SUM(volume_ml_total) AS total
            FROM coleta
            GROUP BY DATE_FORMAT(data_hora, '%Y-%m')
            ORDER BY label
            """;

        Map<String, Double> values = new LinkedHashMap<>();

        try (Connection connection = DatabaseConfig.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                values.put(
                        resultSet.getString("label"),
                        resultSet.getDouble("total")
                );
            }

            return values;

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Nao foi possivel carregar o volume de coletas por mes.",
                    exception
                );
            }
        }

    public double percentageAuthorizedAnimals() {
        String sql = """
                SELECT
                    COALESCE(
                        100.0 * SUM(CASE WHEN autorizacao_doacao = TRUE
                                THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0),
                        0
                    ) AS percentual
                FROM animal_doador
                """;

        try (Connection connection = DatabaseConfig.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()) {

            resultSet.next();
            return resultSet.getDouble("percentual");

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Nao foi possivel calcular o percentual de animais autorizados.",
                    exception
            );
        }
    }

    private DashboardMetric countSingle(String label, String sql) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return new DashboardMetric(label, resultSet.getInt(1));
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel carregar indicador.", exception);
        }
    }
    public Map<String, Double> getAnimalWeightStatistics() {
    Map<String, Double> statistics = new LinkedHashMap<>();

    String sqlAggregate = """
            SELECT
                AVG(peso) AS media,
                VAR_POP(peso) AS variancia,
                STDDEV_POP(peso) AS desvio_padrao
            FROM animal_doador
            WHERE peso IS NOT NULL
            """;

    String sqlMedian = """
            SELECT AVG(peso) AS mediana
            FROM (
                SELECT
                    peso,
                    ROW_NUMBER() OVER (ORDER BY peso) AS posicao,
                    COUNT(*) OVER () AS total
                FROM animal_doador
                WHERE peso IS NOT NULL
            ) AS valores
            WHERE posicao IN (
                CEIL(total / 2.0),
                FLOOR(total / 2.0) + 1
            )
            """;

    String sqlMode = """
        SELECT peso AS moda
        FROM animal_doador
        WHERE peso IS NOT NULL
        GROUP BY peso
        HAVING COUNT(*) > 1
        ORDER BY COUNT(*) DESC, peso ASC
        LIMIT 1
        """;

    try (Connection connection = DatabaseConfig.getConnection()) {

        // Media, variancia e desvio padrao
        try (PreparedStatement statement =
                     connection.prepareStatement(sqlAggregate);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                putIfPresent(statistics, "media",
                        resultSet, "media");

                putIfPresent(statistics, "variancia",
                        resultSet, "variancia");

                putIfPresent(statistics, "desvio_padrao",
                        resultSet, "desvio_padrao");
            }
        }

        // Mediana
        try (PreparedStatement statement =
                     connection.prepareStatement(sqlMedian);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                putIfPresent(statistics, "mediana",
                        resultSet, "mediana");
            }
        }

        // Moda
        try (PreparedStatement statement =
                     connection.prepareStatement(sqlMode);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                putIfPresent(statistics, "moda",
                        resultSet, "moda");
            }
        }

        return statistics;

    } catch (SQLException exception) {
        throw new DatabaseException(
                "Nao foi possivel calcular as estatisticas dos pesos.",
                exception
        );
    }
}

private void putIfPresent(Map<String, Double> values,String key, ResultSet resultSet, String column) throws SQLException {

    Object value = resultSet.getObject(column);

    if (value instanceof Number number) {
        values.put(key, number.doubleValue());
    }
}
    
}
