package br.com.hemopet.dao;

import br.com.hemopet.config.DatabaseConfig;
import br.com.hemopet.model.DashboardFilter;
import br.com.hemopet.model.DashboardMetric;
import java.sql.*;
import java.util.*;

public class DashboardDao {
    private final DashboardFilter filter;
    private static final String SCOPE = """
            WITH fa AS (
                SELECT * FROM animal_doador
                WHERE (? IS NULL OR especie = ?) AND (? IS NULL OR autorizacao_doacao = ?)
            ), fc AS (
                SELECT c.* FROM coleta c JOIN fa a ON a.id_animal = c.id_animal
                WHERE (? IS NULL OR c.data_hora >= ?) AND (? IS NULL OR c.data_hora < ?)
            ), fb AS (
                SELECT b.* FROM bolsa_hemocomponente b JOIN fc c ON c.id_coleta = b.id_coleta
            ), fs AS (
                SELECT s.* FROM solicitacao s
                WHERE (? IS NULL OR s.data_solicitacao >= ?) AND (? IS NULL OR s.data_solicitacao < ?)
                  AND (? = FALSE OR EXISTS (
                    SELECT 1 FROM item_solicitacao i
                    JOIN bolsa_hemocomponente b ON b.id_bolsa = i.id_bolsa
                    JOIN coleta c ON c.id_coleta = b.id_coleta
                    JOIN fa a ON a.id_animal = c.id_animal
                    WHERE i.id_solicitacao = s.id_solicitacao
                  ))
            )
            """;

    public DashboardDao() { this(DashboardFilter.ALL); }
    public DashboardDao(DashboardFilter filter) { this.filter = Objects.requireNonNull(filter); }

    private PreparedStatement prepare(Connection connection, String sql) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(SCOPE + sql);
        Object start = filter.start() == null ? null : filter.start().atStartOfDay();
        Object end = filter.end() == null ? null : filter.end().plusDays(1).atStartOfDay();
        Object[] params = {filter.species(), filter.species(), filter.authorized(), filter.authorized(),
                start, start, end, end, start, start, end, end,
                filter.species() != null || filter.authorized() != null};
        for (int i = 0; i < params.length; i++) statement.setObject(i + 1, params[i]);
        return statement;
    }

    public DashboardMetric countAnimals() { return count("Animais", "SELECT COUNT(*) FROM fa"); }
    public DashboardMetric countHospitals() { return count("Hospitais (geral)", "SELECT COUNT(*) FROM hospital_veterinario"); }
    public DashboardMetric countPeople() { return count("Pessoas (geral)", "SELECT COUNT(*) FROM pessoa"); }
    public DashboardMetric countVeterinarians() { return count("Veterinarios (geral)", "SELECT COUNT(*) FROM veterinario"); }
    public DashboardMetric countCollections() { return count("Coletas", "SELECT COUNT(*) FROM fc"); }
    public DashboardMetric countPendingRequests() { return count("Solicitacoes pendentes", "SELECT COUNT(*) FROM fs WHERE status_solicitacao = 'PENDENTE'"); }
    public DashboardMetric countAvailableBags() { return count("Bolsas em estoque", "SELECT COUNT(*) FROM fb WHERE status_bolsa = 'EM_ESTOQUE'"); }

    private DashboardMetric count(String label, String sql) {
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement s = prepare(c, sql);
             ResultSet r = s.executeQuery()) {
            r.next();
            return new DashboardMetric(label, r.getInt(1));
        } catch (SQLException e) { throw new DatabaseException("Nao foi possivel carregar indicador.", e); }
    }

    public Map<String, Integer> countAnimalsBySpecies() {
        return grouped("SELECT especie AS label, COUNT(*) AS total FROM fa GROUP BY especie ORDER BY especie");
    }
    public Map<String, Integer> countBagsByStatus() {
        return grouped("SELECT status_bolsa AS label, COUNT(*) AS total FROM fb GROUP BY status_bolsa ORDER BY status_bolsa");
    }
    public Map<String, Integer> countRequestsByUrgency() {
        return grouped("SELECT urgencia AS label, COUNT(*) AS total FROM fs GROUP BY urgencia ORDER BY urgencia");
    }
    public Map<String, Integer> countCollectionsByApprovalStatus() {
        return grouped("SELECT status_aprovacao AS label, COUNT(*) AS total FROM fc GROUP BY status_aprovacao ORDER BY status_aprovacao");
    }
    public Map<String, Integer> countAnimalsByWeightRange() {
        return grouped("""
                SELECT CASE WHEN peso < 5 THEN '0-5 kg' WHEN peso < 10 THEN '5-10 kg'
                    WHEN peso < 20 THEN '10-20 kg' WHEN peso < 30 THEN '20-30 kg' ELSE '30+ kg' END AS label,
                    COUNT(*) AS total FROM fa GROUP BY label ORDER BY MIN(peso)
                """);
    }
    private Map<String, Integer> grouped(String sql) {
        Map<String, Integer> values = new LinkedHashMap<>();
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement s = prepare(c, sql);
             ResultSet r = s.executeQuery()) {
            while (r.next()) values.put(r.getString("label"), r.getInt("total"));
            return values;
        } catch (SQLException e) { throw new DatabaseException("Nao foi possivel carregar grafico.", e); }
    }
    public Map<String, Double> sumCollectionVolumeByMonth() {
        Map<String, Double> values = new LinkedHashMap<>();
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement s = prepare(c, """
                SELECT DATE_FORMAT(data_hora, '%Y-%m') AS label, SUM(volume_ml_total) AS total
                FROM fc GROUP BY label ORDER BY label
                """); ResultSet r = s.executeQuery()) {
            while (r.next()) values.put(r.getString("label"), r.getDouble("total"));
            return values;
        } catch (SQLException e) { throw new DatabaseException("Nao foi possivel carregar volume mensal.", e); }
    }
    public double percentageAuthorizedAnimals() {
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement s = prepare(c,
                "SELECT COALESCE(100.0 * SUM(autorizacao_doacao) / NULLIF(COUNT(*), 0), 0) FROM fa");
             ResultSet r = s.executeQuery()) {
            r.next(); return r.getDouble(1);
        } catch (SQLException e) { throw new DatabaseException("Nao foi possivel calcular percentual.", e); }
    }
    public Map<String, Double> getAnimalWeightStatistics() {
        Map<String, Double> values = new LinkedHashMap<>();
        String[] sql = {
            "SELECT AVG(peso) AS media, VAR_POP(peso) AS variancia, STDDEV_POP(peso) AS desvio_padrao FROM fa",
            """
            SELECT AVG(peso) AS mediana FROM (
                SELECT peso, ROW_NUMBER() OVER (ORDER BY peso) AS posicao, COUNT(*) OVER () AS total FROM fa
            ) w WHERE posicao IN (CEIL(total / 2.0), FLOOR(total / 2.0) + 1)
            """,
            "SELECT peso AS moda FROM fa GROUP BY peso HAVING COUNT(*) > 1 ORDER BY COUNT(*) DESC, peso LIMIT 1"
        };
        try (Connection c = DatabaseConfig.getConnection()) {
            for (String query : sql) {
                try (PreparedStatement s = prepare(c, query); ResultSet r = s.executeQuery()) {
                    if (r.next()) {
                        for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) {
                            Object value = r.getObject(i);
                            if (value instanceof Number n) values.put(r.getMetaData().getColumnLabel(i), n.doubleValue());
                        }
                    }
                }
            }
            return values;
        } catch (SQLException e) { throw new DatabaseException("Nao foi possivel calcular estatisticas.", e); }
    }
}
