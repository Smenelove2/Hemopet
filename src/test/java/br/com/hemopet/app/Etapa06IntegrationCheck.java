package br.com.hemopet.app;

import br.com.hemopet.config.DatabaseConfig;
import br.com.hemopet.dao.*;
import br.com.hemopet.model.*;
import br.com.hemopet.service.*;
import br.com.hemopet.view.*;
import org.jfree.chart.ChartPanel;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.Container;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Integracao real; executar somente no schema descartavel preparado pelo script. */
public final class Etapa06IntegrationCheck {
    public static void main(String[] args) throws Exception {
        String url = System.getenv("HEMOPET_DB_URL");
        require(url != null && url.matches(".*[/]hemopet_test_[a-z0-9]+([?].*)?"), "Banco de teste isolado obrigatorio.");
        verifyPeopleAndVeterinarians();
        verifyDashboard();
        verifyQueries();
        verifyRoutines();
        verifyTablesAndCharts();
        System.out.println("Etapa 06 integration check: OK (4 CRUDs, 10 consultas/views, filtros, estatisticas, 6 graficos, funcoes, procedures, triggers e rollback)");
    }

    private static void verifyPeopleAndVeterinarians() throws Exception {
        PessoaService people = new PessoaService();
        VeterinarioService vets = new VeterinarioService();
        Pessoa person = new Pessoa();
        person.setCpf("99888777666"); person.setNome("Pessoa Integracao"); person.setRua("Rua Teste");
        person.setNumero("12"); person.setBairro("Centro"); person.setCidade("Recife"); person.setCep("50000000");
        people.create(person);
        require(scalar("SELECT COUNT(*) FROM pessoa WHERE cpf='99888777666'") == 1, "Inclusao de pessoa.");
        person.setNome("Pessoa Atualizada"); people.update(person);
        require(scalar("SELECT COUNT(*) FROM pessoa WHERE nome='Pessoa Atualizada'") == 1, "Edicao de pessoa.");
        Veterinario vet = new Veterinario();
        vet.setCpf(person.getCpf()); vet.setCrmv("TESTE-ETAPA06"); vets.create(vet);
        require(scalar("SELECT COUNT(*) FROM veterinario WHERE crmv='TESTE-ETAPA06'") == 1, "Inclusao de veterinario.");
        vet.setCrmv("TESTE-ALTERADO"); vets.update(vet);
        require(scalar("SELECT COUNT(*) FROM veterinario WHERE crmv='TESTE-ALTERADO'") == 1, "Edicao de veterinario.");
        vet.setCpfSupervisor(vet.getCpf());
        expect(ValidationException.class, () -> vets.update(vet), "Auto supervisao deve ser rejeitada.");
        vets.delete(person.getCpf()); people.delete(person.getCpf());
        require(scalar("SELECT COUNT(*) FROM pessoa WHERE cpf='99888777666'") == 0, "Exclusao de pessoa.");
        require(scalar("SELECT COUNT(*) FROM veterinario WHERE cpf='99888777666'") == 0, "Exclusao de veterinario.");
    }

    private static void verifyDashboard() throws Exception {
        DashboardDao all = new DashboardDao();
        DashboardDao dogs = new DashboardDao(new DashboardFilter("CAO", true, null, null));
        require(dogs.countAnimals().value() == scalar("SELECT COUNT(*) FROM animal_doador WHERE especie='CAO' AND autorizacao_doacao=TRUE"), "Filtro especie + autorizacao.");
        require(dogs.countAnimalsBySpecies().keySet().equals(Set.of("CAO")), "Grafico deve acompanhar filtro.");
        require(dogs.countCollections().value() == scalar("""
                SELECT COUNT(*) FROM coleta c JOIN animal_doador a ON a.id_animal=c.id_animal
                WHERE a.especie='CAO' AND a.autorizacao_doacao=TRUE
                """), "Filtro de coletas.");
        require(dogs.countAvailableBags().value() == scalar("""
                SELECT COUNT(*) FROM bolsa_hemocomponente b JOIN coleta c ON c.id_coleta=b.id_coleta
                JOIN animal_doador a ON a.id_animal=c.id_animal WHERE a.especie='CAO'
                AND a.autorizacao_doacao=TRUE AND b.status_bolsa='EM_ESTOQUE'
                """), "Filtro de estoque.");
        Map<String, Double> stats = dogs.getAnimalWeightStatistics();
        List<Double> weights = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT peso FROM animal_doador WHERE especie='CAO' AND autorizacao_doacao=TRUE ORDER BY peso")) {
            while (r.next()) weights.add(r.getDouble(1));
        }
        double mean = weights.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double variance = weights.stream().mapToDouble(w -> (w-mean)*(w-mean)).average().orElseThrow();
        double median = (weights.get((weights.size()-1)/2) + weights.get(weights.size()/2)) / 2;
        close(stats.get("media"), mean, "Media"); close(stats.get("mediana"), median, "Mediana");
        close(stats.get("variancia"), variance, "Variancia"); close(stats.get("desvio_padrao"), Math.sqrt(variance), "Desvio");
        DashboardDao empty = new DashboardDao(new DashboardFilter(null, null, LocalDate.of(2099,1,1), LocalDate.of(2099,1,1)));
        require(empty.countCollections().value() == 0 && empty.countAvailableBags().value() == 0
                && empty.countPendingRequests().value() == 0, "Periodo vazio deve zerar indicadores.");
        require(empty.sumCollectionVolumeByMonth().isEmpty() && empty.countBagsByStatus().isEmpty()
                && empty.countRequestsByUrgency().isEmpty() && empty.countCollectionsByApprovalStatus().isEmpty(), "Periodo vazio nos graficos.");
        require(empty.countAnimals().value() == all.countAnimals().value(), "Periodo nao filtra nascimento de animais.");
        require(!all.countAnimalsByWeightRange().isEmpty() && !all.sumCollectionVolumeByMonth().isEmpty(), "Graficos com dados reais.");
        expect(ValidationException.class, () -> new DashboardFilter(null,null,LocalDate.of(2026,2,2),LocalDate.of(2026,1,1)), "Periodo invertido.");
    }

    private static void verifyQueries() {
        QueryDao queries = new QueryDao();
        require(queries.getQueryNames().size() == 10, "Oito consultas e duas views.");
        queries.getQueryNames().keySet().forEach(name -> {
            require(queries.execute(name).getColumnCount() > 0, name);
            require(queries.execute(name, new QueryFilter("' OR 1=1 --",null,null,null,null,null)).getRowCount() == 0, "Busca deve ser literal: " + name);
        });
        DefaultTableModel dogs = queries.execute("Etapa 03 - Animais por especie", new QueryFilter("","CAO",null,null,null,null));
        require(dogs.getRowCount() == 1 && "CAO".equals(dogs.getValueAt(0,0)), "Filtro SQL de especie.");
        DefaultTableModel stock = queries.execute("View - Rastreabilidade da doacao", new QueryFilter("","CAO","EM_ESTOQUE",null,null,null));
        for (int row=0; row<stock.getRowCount(); row++) {
            require("CAO".equals(stock.getValueAt(row,stock.findColumn("especie"))), "View filtrada por especie.");
            require("EM_ESTOQUE".equals(stock.getValueAt(row,stock.findColumn("status_bolsa"))), "View filtrada por status.");
        }
        require(queries.execute("View - Solicitacoes por hospital", new QueryFilter("",null,null,"ALTA",LocalDate.of(2099,1,1),LocalDate.of(2099,1,2))).getRowCount() == 0, "Parametros de data na view.");
        expect(IllegalArgumentException.class, () -> queries.execute("INVALIDA"), "Consulta desconhecida.");
    }

    private static void verifyRoutines() throws Exception {
        Etapa5Dao dao = new Etapa5Dao();
        int animal = scalar("SELECT MIN(id_animal) FROM animal_doador WHERE autorizacao_doacao=TRUE");
        require(dao.consultarIdadeAnimal(animal) == scalar("SELECT TIMESTAMPDIFF(YEAR,data_nascimento,CURDATE()) FROM animal_doador WHERE id_animal="+animal), "Funcao idade.");
        require(dao.consultarIdadeAnimal(Integer.MAX_VALUE) == null, "Animal inexistente.");
        require("INEXISTENTE".equals(dao.consultarStatusValidade(Integer.MAX_VALUE)), "Bolsa inexistente.");
        require(dao.testarTriggerColeta().startsWith("Trigger confirmado"), "Bloqueio de coleta pelo trigger.");

        int collection = insert("""
                INSERT INTO coleta(data_hora,volume_ml_total,status_aprovacao,id_animal,cpf_veterinario)
                SELECT NOW(),250,'APROVADA',a.id_animal,v.cpf FROM animal_doador a CROSS JOIN veterinario v
                WHERE a.autorizacao_doacao=TRUE LIMIT 1
                """);
        int valid = bag(collection, 30, "EM_ESTOQUE");
        int expired = bag(collection, -1, "RESERVADA");
        int soon = bag(collection, 3, "EM_ESTOQUE");
        require("VALIDA".equals(dao.consultarStatusValidade(valid)), "Validade futura.");
        require("VENCIDA".equals(dao.consultarStatusValidade(expired)), "Validade passada.");
        require("VENCE_EM_7_DIAS".equals(dao.consultarStatusValidade(soon)), "Vencimento proximo.");
        int request = request();
        item(request,valid); item(request,expired);
        int beforeLogs = scalar("SELECT COUNT(*) FROM log_bolsa");
        List<DefaultTableModel> results = dao.processarSolicitacao(request);
        require(results.size()==2 && results.get(0).getRowCount()==1 && results.get(1).getRowCount()==1, "Dois resultados do cursor.");
        require("PENDENTE".equals(value(results.get(0),"status_final")), "Problemas mantem solicitacao pendente.");
        require(scalar("SELECT COUNT(*) FROM log_bolsa")==beforeLogs+2, "Trigger de log para os dois status.");
        require(scalar("SELECT COUNT(*) FROM bolsa_hemocomponente WHERE id_bolsa="+valid+" AND status_bolsa='DISTRIBUIDA'")==1, "Distribuicao.");
        require(scalar("SELECT COUNT(*) FROM bolsa_hemocomponente WHERE id_bolsa="+expired+" AND status_bolsa='DESCARTADA'")==1, "Descarte.");
        results=dao.processarSolicitacao(request);
        require(((Number)value(results.get(0),"bolsas_ja_entregues")).intValue()==1, "Reprocessamento nao distribui novamente.");

        int cleanRequest = request(); item(cleanRequest,soon);
        results=dao.processarSolicitacao(cleanRequest);
        require("ATENDIDA".equals(value(results.get(0),"status_final")) && results.get(1).getRowCount()==0, "Solicitacao atendida.");
        expect(DatabaseException.class, () -> dao.processarSolicitacao(cleanRequest), "Solicitacao atendida nao pode ser reprocessada.");
        int emptyRequest = request();
        expect(DatabaseException.class, () -> dao.processarSolicitacao(emptyRequest), "Solicitacao sem itens.");
        expect(DatabaseException.class, () -> dao.processarSolicitacao(Integer.MAX_VALUE), "Solicitacao inexistente.");

        int first = bag(collection,30,"EM_ESTOQUE"), second = bag(collection,30,"EM_ESTOQUE");
        int rollbackRequest = request(); item(rollbackRequest,first); item(rollbackRequest,second);
        beforeLogs=scalar("SELECT COUNT(*) FROM log_bolsa");
        execute("CREATE TRIGGER test_force_failure BEFORE UPDATE ON bolsa_hemocomponente FOR EACH ROW BEGIN IF NEW.id_bolsa="+second+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Falha injetada'; END IF; END");
        try {
            expect(DatabaseException.class, () -> dao.processarSolicitacao(rollbackRequest), "Falha no segundo item.");
            require(scalar("SELECT COUNT(*) FROM bolsa_hemocomponente WHERE id_bolsa="+first+" AND status_bolsa='EM_ESTOQUE'")==1, "Rollback do primeiro item.");
            require(scalar("SELECT COUNT(*) FROM log_bolsa")==beforeLogs, "Rollback do log.");
        } finally { execute("DROP TRIGGER test_force_failure"); }

        bag(collection,-1,"EM_ESTOQUE");
        bag(collection,-1,"DISTRIBUIDA");
        int expected=scalar("SELECT COUNT(*) FROM bolsa_hemocomponente WHERE status_bolsa IN ('EM_ESTOQUE','RESERVADA') AND data_validade<CURDATE()");
        int discarded=dao.descartarBolsasVencidas(null);
        require(discarded==expected && discarded>0, "Parametro OUT do descarte.");
        require(dao.descartarBolsasVencidas(null)==0, "Descarte repetido nao duplica.");
        require(dao.listLogBolsa().getRowCount()>0 && dao.listBolsasDescartadas().getRowCount()>0, "Resultados da Etapa 05.");

        LocalDate today;
        try(Connection c=DatabaseConfig.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT CURDATE()")){r.next();today=r.getDate(1).toLocalDate();}
        execute("UPDATE coleta SET data_hora=TIMESTAMP(CURDATE(),'23:59:59') WHERE id_coleta="+collection);
        QueryDao queries=new QueryDao();
        DefaultTableModel sameDay=queries.execute("View - Rastreabilidade da doacao",new QueryFilter("",null,null,null,today,today));
        boolean found=false;
        for(int row=0;row<sameDay.getRowCount();row++) if(((Number)sameDay.getValueAt(row,sameDay.findColumn("id_bolsa"))).intValue()==valid)found=true;
        require(found,"Data final inclui 23:59:59.");
    }

    private static void verifyTablesAndCharts() throws Exception {
        AtomicReference<DashboardPanel> dashboard = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            JTable table=new JTable(new DefaultTableModel(new Object[][]{{"Zulu",2},{"Alpha",1}},new Object[]{"nome","id"}));
            TableFilterPanel filter=new TableFilterPanel(table);
            JTextField search=find(filter,JTextField.class).get(0);
            search.setText("Alpha");
            require(table.getRowCount()==1 && table.convertRowIndexToModel(0)==1,"Busca preserva indice do formulario.");
            search.setText("["); require(table.getRowCount()==0,"Busca literal nao interpreta regex.");
            search.setText(""); require(table.getRowCount()==2,"Limpar busca.");
            dashboard.set(new DashboardPanel());
        });
        long deadline=System.currentTimeMillis()+20000;
        while(System.currentTimeMillis()<deadline) {
            int[] count={0};
            SwingUtilities.invokeAndWait(()-> count[0]=find(dashboard.get(),ChartPanel.class).size());
            if(count[0]==6) return;
            Thread.sleep(100);
        }
        throw new IllegalStateException("Dashboard deve carregar seis graficos.");
    }

    private static <T> List<T> find(Container parent,Class<T> type){
        List<T> result=new ArrayList<>();
        for(Component c:parent.getComponents()){if(type.isInstance(c)) result.add(type.cast(c)); if(c instanceof Container nested)result.addAll(find(nested,type));}
        return result;
    }
    private static int bag(int collection,int days,String status)throws Exception{
        return insert("INSERT INTO bolsa_hemocomponente(tipo_componente,data_fabricacao,data_validade,status_bolsa,id_coleta) VALUES('SANGUE_TOTAL',CURDATE()-INTERVAL 30 DAY,CURDATE()+INTERVAL "+days+" DAY,'"+status+"',"+collection+")");
    }
    private static int request()throws Exception{
        return insert("INSERT INTO solicitacao(id_hospital) SELECT MIN(id_hospital) FROM hospital_veterinario");
    }
    private static void item(int request,int bag)throws Exception{
        execute("INSERT INTO item_solicitacao(quantidade,ht_percentual,id_solicitacao,id_bolsa) VALUES(1,40,"+request+","+bag+")");
    }
    private static int insert(String sql)throws Exception{
        try(Connection c=DatabaseConfig.getConnection();Statement s=c.createStatement()){
            s.executeUpdate(sql,Statement.RETURN_GENERATED_KEYS);
            try(ResultSet r=s.getGeneratedKeys()){require(r.next(),"Chave gerada.");return r.getInt(1);}
        }
    }
    private static void execute(String sql)throws Exception{try(Connection c=DatabaseConfig.getConnection();Statement s=c.createStatement()){s.execute(sql);}}
    private static int scalar(String sql)throws Exception{try(Connection c=DatabaseConfig.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getInt(1);}}
    private static Object value(DefaultTableModel model,String column){return model.getValueAt(0,model.findColumn(column));}
    private static void close(Double actual,double expected,String message){require(actual!=null && Math.abs(actual-expected)<0.000001,message);}
    private static void expect(Class<? extends RuntimeException> type,Runnable action,String message){
        try{action.run();}catch(RuntimeException e){if(type.isInstance(e))return;throw e;}
        throw new IllegalStateException(message);
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
