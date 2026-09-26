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
}
