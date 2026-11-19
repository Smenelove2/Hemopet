package br.com.hemopet.dao;

import br.com.hemopet.config.DatabaseConfig;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Etapa5Dao {
    public Integer consultarIdadeAnimal(int id) {
        Number age = (Number) scalar("SELECT fn_idade_animal(?)", id);
        return age == null ? null : age.intValue();
    }
    public String consultarStatusValidade(int id) {
        return (String) scalar("SELECT fn_status_validade(?)", id);
    }
    private Object scalar(String sql, int id) {
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) { r.next(); return r.getObject(1); }
        } catch (SQLException e) { throw new DatabaseException("Nao foi possivel consultar a funcao do banco.", e); }
    }
    public int descartarBolsasVencidas(LocalDate reference) {
        try (Connection c = DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try (CallableStatement s = c.prepareCall("{call sp_descartar_bolsas_vencidas(?, ?)}")) {
                s.setObject(1, reference);
                s.registerOutParameter(2, Types.INTEGER);
                s.execute();
                int total = s.getInt(2);
                c.commit();
                return total;
            } catch (SQLException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw new DatabaseException("Nao foi possivel descartar bolsas vencidas.", e); }
    }
    public List<DefaultTableModel> processarSolicitacao(int id) {
        try (Connection c = DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try (CallableStatement s = c.prepareCall("{call sp_processar_solicitacao(?)}")) {
                s.setInt(1, id);
                List<DefaultTableModel> results = new ArrayList<>();
                boolean hasResult = s.execute();
                while (true) {
                    if (hasResult) {
                        try (ResultSet r = s.getResultSet()) { results.add(ResultSetTableMapper.toTableModel(r)); }
                    } else if (s.getUpdateCount() == -1) break;
                    hasResult = s.getMoreResults();
                }
                if (results.size() != 2) throw new SQLException("O procedimento deve retornar resumo e problemas.");
                c.commit();
                return results;
            } catch (SQLException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw new DatabaseException("Nao foi possivel processar a solicitacao.", e); }
    }
    public DefaultTableModel listLogBolsa() {
        return table("SELECT id_log, id_bolsa, status_anterior, status_novo, data_hora, usuario FROM log_bolsa ORDER BY id_log DESC");
    }
    public DefaultTableModel listBolsasDescartadas() {
        return table("SELECT id_bolsa, tipo_componente, data_validade, status_bolsa FROM bolsa_hemocomponente WHERE status_bolsa = 'DESCARTADA' ORDER BY id_bolsa");
    }
    private DefaultTableModel table(String sql) {
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) { return ResultSetTableMapper.toTableModel(r); }
        catch (SQLException e) { throw new DatabaseException("Nao foi possivel listar os resultados da Etapa 05.", e); }
    }
    public String testarTriggerColeta() {
        // A demonstracao sempre reverte a transacao, inclusive se o trigger estiver ausente.
        try (Connection c = DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement s = c.prepareStatement("""
                    INSERT INTO coleta (data_hora, volume_ml_total, status_aprovacao, id_animal, cpf_veterinario)
                    SELECT NOW(), 100, 'APROVADA', a.id_animal, v.cpf FROM animal_doador a
                    CROSS JOIN veterinario v WHERE a.autorizacao_doacao = FALSE LIMIT 1
                    """)) {
                int inserted = s.executeUpdate();
                return inserted == 0 ? "Cadastre um animal sem autorizacao e um veterinario para demonstrar."
                        : "O trigger nao bloqueou a coleta. A demonstracao foi revertida.";
            } catch (SQLException e) {
                if ("45000".equals(e.getSQLState()) && e.getMessage().contains("Coleta bloqueada"))
                    return "Trigger confirmado: coleta sem autorizacao bloqueada pelo banco.";
                throw e;
            } finally { c.rollback(); }
        } catch (SQLException e) { throw new DatabaseException("Nao foi possivel demonstrar o trigger de coleta.", e); }
    }
}
