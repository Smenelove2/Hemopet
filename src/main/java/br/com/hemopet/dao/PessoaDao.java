
package br.com.hemopet.dao;

import br.com.hemopet.config.DatabaseConfig;
import br.com.hemopet.model.Pessoa;

import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PessoaDao {

    public DefaultTableModel listTableModel() {
        String sql = """
                SELECT cpf, nome, rua, numero, bairro, cidade, cep
                FROM pessoa
                ORDER BY nome
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            return ResultSetTableMapper.toTableModel(resultSet);

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Nao foi possivel listar pessoas.", exception);
        }
    }

    public void insert(Pessoa pessoa) {
        String sql = """
                INSERT INTO pessoa
                (cpf, nome, rua, numero, bairro, cidade, cep)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            fillStatement(statement, pessoa, false);
            statement.executeUpdate();

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Nao foi possivel inserir pessoa.", exception);
        }
    }

    public void update(Pessoa pessoa) {
        String sql = """
                UPDATE pessoa
                SET nome = ?, rua = ?, numero = ?, bairro = ?,
                    cidade = ?, cep = ?
                WHERE cpf = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            fillStatement(statement, pessoa, true);
            statement.executeUpdate();

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Nao foi possivel atualizar pessoa.", exception);
        }
    }

    public void delete(String cpf) {
        String sql = "DELETE FROM pessoa WHERE cpf = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, cpf);
            statement.executeUpdate();

        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Nao foi possivel excluir pessoa. Verifique se existem registros vinculados.",
                    exception);
        }
    }

    private void fillStatement(
            PreparedStatement statement,
            Pessoa pessoa,
            boolean isUpdate
    ) throws SQLException {

        int index = 1;

        if (!isUpdate) {
            statement.setString(index++, pessoa.getCpf());
        }

        statement.setString(index++, pessoa.getNome());
        statement.setString(index++, pessoa.getRua());
        statement.setString(index++, pessoa.getNumero());
        statement.setString(index++, pessoa.getBairro());
        statement.setString(index++, pessoa.getCidade());
        statement.setString(index++, pessoa.getCep());

        if (isUpdate) {
            statement.setString(index, pessoa.getCpf());
        }
    }
}
