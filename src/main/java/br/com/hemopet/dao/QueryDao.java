package br.com.hemopet.dao;

import br.com.hemopet.config.DatabaseConfig;
import br.com.hemopet.model.QueryFilter;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.*;

public class QueryDao {
    private record Spec(String sql, String text, String species, String status, String urgency, String date) { }
    private static final Map<String, Spec> QUERIES = new LinkedHashMap<>();
    static {
        add("Etapa 03 - Animais por especie", """
                SELECT a.especie, COUNT(*) AS quantidade_animais_por_especie
                FROM animal_doador a WHERE 1=1 /*filters*/ GROUP BY a.especie ORDER BY a.especie
                """, "a.especie", "a.especie", null, null, null);
        add("Etapa 03 - Animais e tutores", """
                SELECT p.nome AS tutor, a.nome AS animal, a.especie
                FROM pessoa p JOIN tutor t ON t.cpf=p.cpf JOIN animal_doador a ON a.cpf_tutor=t.cpf
                WHERE 1=1 /*filters*/ ORDER BY p.nome, a.nome
                """, "p.nome, p.cpf, a.nome", "a.especie", null, null, null);
        add("Etapa 03 - Animais acima da media de peso", """
                SELECT a.nome AS animal, p.nome AS tutor, a.peso, a.especie
                FROM pessoa p JOIN tutor t ON p.cpf=t.cpf JOIN animal_doador a ON a.cpf_tutor=t.cpf
                WHERE a.peso > (SELECT AVG(peso) FROM animal_doador) /*filters*/ ORDER BY a.peso DESC, a.id_animal
                """, "a.nome, p.nome, p.cpf", "a.especie", null, null, null);
        add("Etapa 03 - Tutores com cao e gato", """
                SELECT p.nome AS tutor, SUM(a.especie='CAO') AS quantidade_caes, SUM(a.especie='GATO') AS quantidade_gatos
                FROM pessoa p JOIN tutor t ON p.cpf=t.cpf JOIN animal_doador a ON a.cpf_tutor=t.cpf
                WHERE 1=1 /*filters*/ GROUP BY p.cpf, p.nome
                HAVING SUM(a.especie='CAO')>0 AND SUM(a.especie='GATO')>0 ORDER BY p.nome
                """, "p.nome, p.cpf", null, null, null, null);
        add("Etapa 04 - Tutores sem animais (anti join)", """
                SELECT p.cpf, p.nome, p.cidade FROM tutor t JOIN pessoa p ON p.cpf=t.cpf
                LEFT JOIN animal_doador a ON a.cpf_tutor=t.cpf
                WHERE a.id_animal IS NULL /*filters*/ ORDER BY p.nome
                """, "p.cpf, p.nome, p.cidade", null, null, null, null);
        add("Etapa 04 - Hospitais e solicitacoes (full outer join)", """
                SELECT * FROM (
                    SELECT h.nome_fantasia, h.cnpj, s.id_solicitacao, s.urgencia, s.status_solicitacao, s.data_solicitacao
                    FROM hospital_veterinario h LEFT JOIN solicitacao s ON s.id_hospital=h.id_hospital
                    UNION
                    SELECT h.nome_fantasia, h.cnpj, s.id_solicitacao, s.urgencia, s.status_solicitacao, s.data_solicitacao
                    FROM hospital_veterinario h RIGHT JOIN solicitacao s ON s.id_hospital=h.id_hospital
                ) u WHERE 1=1 /*filters*/ ORDER BY u.nome_fantasia, u.id_solicitacao
                """, "u.nome_fantasia, u.cnpj", null, null, "u.urgencia", "u.data_solicitacao");
        add("Etapa 04 - Coletas acima da media de volume", """
                SELECT c.id_coleta, c.data_hora, c.volume_ml_total, a.nome AS nome_animal, a.especie
                FROM coleta c JOIN animal_doador a ON a.id_animal=c.id_animal
                WHERE c.volume_ml_total > (SELECT AVG(volume_ml_total) FROM coleta)
                /*filters*/ ORDER BY c.volume_ml_total DESC, c.id_coleta
                """, "a.nome", "a.especie", null, null, "c.data_hora");
        add("Etapa 04 - Veterinarios com coletas aprovadas", """
                SELECT p.nome AS nome_veterinario, v.crmv FROM veterinario v JOIN pessoa p ON p.cpf=v.cpf
                WHERE EXISTS (SELECT 1 FROM coleta c WHERE c.cpf_veterinario=v.cpf AND c.status_aprovacao='APROVADA')
                /*filters*/ ORDER BY p.nome
                """, "p.nome, p.cpf, v.crmv", null, null, null, null);
        add("View - Rastreabilidade da doacao", """
                SELECT * FROM vw_rastreabilidade_doacao v WHERE 1=1 /*filters*/ ORDER BY v.id_bolsa
                """, "v.nome_animal, v.nome_tutor, v.nome_veterinario, v.crmv", "v.especie", "v.status_bolsa", null, "v.data_coleta");
        add("View - Solicitacoes por hospital", """
                SELECT * FROM vw_resumo_solicitacoes_hospital v WHERE 1=1 /*filters*/ ORDER BY v.id_solicitacao, v.id_item
                """, "v.hospital, v.cnpj", null, "v.status_bolsa", "v.urgencia", "v.data_solicitacao");
    }
    private static void add(String name, String sql, String text, String species, String status, String urgency, String date) {
        QUERIES.put(name, new Spec(sql, text, species, status, urgency, date));
    }
    private static Spec spec(String name) {
        Spec spec = QUERIES.get(name);
        if (spec == null) throw new IllegalArgumentException("Consulta nao encontrada: " + name);
        return spec;
    }
    public Map<String, String> getQueryNames() {
        Map<String, String> names = new LinkedHashMap<>();
        QUERIES.forEach((name, spec) -> names.put(name, spec.sql()));
        return Collections.unmodifiableMap(names);
    }
    public Set<String> getSupportedFilters(String name) {
        Spec s = spec(name);
        Set<String> filters = new LinkedHashSet<>();
        if (s.text() != null) filters.add("text");
        if (s.species() != null) filters.add("species");
        if (s.status() != null) filters.add("status");
        if (s.urgency() != null) filters.add("urgency");
        if (s.date() != null) filters.add("date");
        return Collections.unmodifiableSet(filters);
    }
    public DefaultTableModel execute(String name) { return execute(name, QueryFilter.ALL); }
    public DefaultTableModel execute(String name, QueryFilter filter) {
        Spec s = spec(name);
        StringBuilder conditions = new StringBuilder();
        List<Object> params = new ArrayList<>();
        if (!filter.text().isBlank() && s.text() != null) {
            conditions.append(" AND LOCATE(?, CONCAT_WS(' ', ").append(s.text()).append(")) > 0");
            params.add(filter.text());
        }
        equal(conditions, params, s.species(), filter.species());
        equal(conditions, params, s.status(), filter.status());
        equal(conditions, params, s.urgency(), filter.urgency());
        if (s.date() != null && filter.start() != null) {
            conditions.append(" AND ").append(s.date()).append(" >= ?");
            params.add(filter.start().atStartOfDay());
        }
        if (s.date() != null && filter.end() != null) {
            conditions.append(" AND ").append(s.date()).append(" < ?");
            params.add(filter.end().plusDays(1).atStartOfDay());
        }
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement statement = c.prepareStatement(s.sql().replace("/*filters*/", conditions))) {
            for (int i = 0; i < params.size(); i++) statement.setObject(i + 1, params.get(i));
            try (ResultSet result = statement.executeQuery()) { return ResultSetTableMapper.toTableModel(result); }
        } catch (SQLException e) { throw new DatabaseException("Nao foi possivel executar consulta.", e); }
    }
    private static void equal(StringBuilder sql, List<Object> params, String column, String value) {
        if (column != null && value != null && !value.isBlank()) {
            sql.append(" AND ").append(column).append(" = ?");
            params.add(value);
        }
    }
}
