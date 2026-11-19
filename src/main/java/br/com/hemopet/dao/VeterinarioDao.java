package br.com.hemopet.dao;

import br.com.hemopet.config.DatabaseConfig;
import br.com.hemopet.model.Veterinario;
import br.com.hemopet.model.OptionItem;

import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class VeterinarioDao {
        public DefaultTableModel listTableModel() {
        String sql = """
                SELECT v.cpf, p.nome, v.crmv,
                    v.cpf_supervisor,
                    ps.nome AS supervisor
                FROM veterinario v
                JOIN pessoa p ON p.cpf = v.cpf
                LEFT JOIN veterinario vs ON vs.cpf = v.cpf_supervisor
                LEFT JOIN pessoa ps ON ps.cpf = vs.cpf
                ORDER BY p.nome;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return ResultSetTableMapper.toTableModel(resultSet);
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel listar veterinários.", exception);
        }
    }

   
    public void insert(Veterinario veterinario) {
        String sql = """
                INSERT veterinario
                (cpf, crmv, cpf_supervisor)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, veterinario, false);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel inserir veterinário.", exception);
        }
    }

    public void update(Veterinario veterinario) {
        String sql = """
            UPDATE veterinario
            SET crmv = ?, cpf_supervisor = ?
            WHERE cpf = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, veterinario, true);
            statement.executeUpdate();
            
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel atualizar veterinario.", exception);
        }
    }
    
    public void delete(String cpfVet) {
        String sql = "DELETE FROM veterinario WHERE cpf = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cpfVet);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Nao foi possivel excluir veterinario. Verifique se ha coletas vinculadas.", exception);
        }
    }    
    

    
public List<OptionItem> listSupervisors(String cpfAtual) {
    String sql = """
            SELECT v.cpf, p.nome
            FROM veterinario v
            JOIN pessoa p ON p.cpf = v.cpf
            WHERE (? IS NULL OR v.cpf <> ?)
            ORDER BY p.nome
            """;

    List<OptionItem> supervisors = new ArrayList<>();

    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

        if (cpfAtual == null || cpfAtual.isBlank()) {
            statement.setNull(1, Types.CHAR);
            statement.setNull(2, Types.CHAR);
        } else {
            statement.setString(1, cpfAtual.trim());
            statement.setString(2, cpfAtual.trim());
        }

        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String cpf = resultSet.getString("cpf");
                String nome = resultSet.getString("nome");

                supervisors.add(new OptionItem(cpf, nome + " (" + cpf + ")"));
            }
        }

        return supervisors;

    } catch (SQLException exception) {
        throw new DatabaseException(
                "Nao foi possivel carregar os supervisores.", exception);
    }
}
    private void setCpfSupervisor(PreparedStatement statement, int index, String cpfSupervisor) throws SQLException {

        if (cpfSupervisor == null || cpfSupervisor.isBlank()) {
            statement.setNull(index, Types.CHAR);
        } else {
            statement.setString(index, cpfSupervisor.trim());
        }
    }

    private void fillStatement(PreparedStatement statement, Veterinario veterinario, boolean isUpdate) throws SQLException {

        int index = 1;

        if (isUpdate == false) {
            statement.setString(index++, veterinario.getCpf());
        }

        statement.setString(index++, veterinario.getCrmv());

        setCpfSupervisor(statement, index++, veterinario.getCpfSupervisor());

        if (isUpdate == true) {
            statement.setString(index, veterinario.getCpf());
        }
    }
}
