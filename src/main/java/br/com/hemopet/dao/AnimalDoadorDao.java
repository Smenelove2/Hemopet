package br.com.hemopet.dao;

import br.com.hemopet.config.DatabaseConfig;
import br.com.hemopet.model.AnimalDoador;
import br.com.hemopet.model.OptionItem;

import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AnimalDoadorDao {
    public DefaultTableModel listTableModel() {
        String sql = """
                SELECT a.id_animal, a.nome, a.especie, a.raca, a.tipo_sanguineo,
                       a.peso, a.data_nascimento, a.autorizacao_doacao,
                       a.cpf_tutor, p.nome AS tutor
                FROM animal_doador a
                JOIN tutor t ON t.cpf = a.cpf_tutor
                JOIN pessoa p ON p.cpf = t.cpf
                ORDER BY a.id_animal
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return ResultSetTableMapper.toTableModel(resultSet);
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel listar animais.", exception);
        }
    }

    public void insert(AnimalDoador animal) {
        String sql = """
                INSERT INTO animal_doador
                (nome, especie, raca, tipo_sanguineo, peso, data_nascimento, autorizacao_doacao, cpf_tutor)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, animal, false);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel inserir animal.", exception);
        }
    }

    public void update(AnimalDoador animal) {
        String sql = """
                UPDATE animal_doador
                SET nome = ?, especie = ?, raca = ?, tipo_sanguineo = ?, peso = ?,
                    data_nascimento = ?, autorizacao_doacao = ?, cpf_tutor = ?
                WHERE id_animal = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, animal, true);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel atualizar animal.", exception);
        }
    }

    public void delete(int idAnimal) {
        String sql = "DELETE FROM animal_doador WHERE id_animal = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idAnimal);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel excluir animal. Verifique se ha coletas vinculadas.", exception);
        }
    }

    public List<OptionItem> listTutors() {
        String sql = """
                SELECT t.cpf, p.nome
                FROM tutor t
                JOIN pessoa p ON p.cpf = t.cpf
                ORDER BY p.nome
                """;
        List<OptionItem> tutors = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String cpf = resultSet.getString("cpf");
                String nome = resultSet.getString("nome");
                tutors.add(new OptionItem(cpf, nome + " (" + cpf + ")"));
            }
            return tutors;
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel carregar tutores.", exception);
        }
    }

    private void fillStatement(PreparedStatement statement, AnimalDoador animal, boolean includeId) throws SQLException {
        statement.setString(1, animal.getNome());
        statement.setString(2, animal.getEspecie());
        statement.setString(3, animal.getRaca());
        statement.setString(4, animal.getTipoSanguineo());
        statement.setBigDecimal(5, animal.getPeso());
        statement.setDate(6, Date.valueOf(animal.getDataNascimento()));
        statement.setBoolean(7, animal.isAutorizacaoDoacao());
        statement.setString(8, animal.getCpfTutor());
        if (includeId) {
            statement.setInt(9, animal.getIdAnimal());
        }
    }
}
