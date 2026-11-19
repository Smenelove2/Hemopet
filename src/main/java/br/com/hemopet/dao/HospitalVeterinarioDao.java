package br.com.hemopet.dao;

import br.com.hemopet.config.DatabaseConfig;
import br.com.hemopet.model.HospitalVeterinario;

import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HospitalVeterinarioDao {
    public DefaultTableModel listTableModel() {
        String sql = """
                SELECT id_hospital, cnpj, nome_fantasia, ativo
                FROM hospital_veterinario
                ORDER BY id_hospital
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return ResultSetTableMapper.toTableModel(resultSet);
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel listar hospitais.", exception);
        }
    }

    public void insert(HospitalVeterinario hospital) {
        String sql = """
                INSERT INTO hospital_veterinario (cnpj, nome_fantasia, ativo)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hospital.getCnpj());
            statement.setString(2, hospital.getNomeFantasia());
            statement.setBoolean(3, hospital.isAtivo());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel inserir hospital.", exception);
        }
    }

    public void update(HospitalVeterinario hospital) {
        String sql = """
                UPDATE hospital_veterinario
                SET cnpj = ?, nome_fantasia = ?, ativo = ?
                WHERE id_hospital = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hospital.getCnpj());
            statement.setString(2, hospital.getNomeFantasia());
            statement.setBoolean(3, hospital.isAtivo());
            statement.setInt(4, hospital.getIdHospital());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel atualizar hospital.", exception);
        }
    }

    public void delete(int idHospital) {
        String sql = "DELETE FROM hospital_veterinario WHERE id_hospital = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idHospital);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel excluir hospital. Verifique se ha solicitacoes vinculadas.", exception);
        }
    }
}
