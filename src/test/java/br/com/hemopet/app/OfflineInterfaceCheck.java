package br.com.hemopet.app;

import br.com.hemopet.dao.QueryDao;
import br.com.hemopet.dao.ResultSetTableMapper;
import br.com.hemopet.model.*;
import br.com.hemopet.service.*;
import br.com.hemopet.view.TableFilterPanel;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.sql.rowset.*;
import java.awt.Component;
import java.awt.Container;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class OfflineInterfaceCheck {
    public static void main(String[] args) throws Exception {
        QueryDao queries = new QueryDao();
        require(queries.getQueryNames().size() == 10, "Oito consultas e duas views no catalogo.");
        require(queries.getSupportedFilters("View - Rastreabilidade da doacao").containsAll(List.of("text","species","status","date")), "Filtros de rastreabilidade.");
        require(queries.getSupportedFilters("View - Solicitacoes por hospital").containsAll(List.of("text","urgency","status","date")), "Filtros da view de hospitais.");
        require(!queries.getSupportedFilters("Etapa 03 - Tutores com cao e gato").contains("species"), "Filtro nao pode alterar o significado da consulta.");
        rejected(() -> new DashboardFilter(null,null,LocalDate.of(2026,2,1),LocalDate.of(2026,1,1)));
        rejected(() -> new DashboardFilter("INVALIDA",null,null,null));
        rejected(() -> new QueryFilter("",null,null,null,LocalDate.of(2026,2,1),LocalDate.of(2026,1,1)));
        rejected(() -> new PessoaService().create(new Pessoa()));
        rejected(() -> new VeterinarioService().delete("12"));
        rejected(() -> new AnimalDoadorService().save(new AnimalDoador()));
        rejected(() -> new HospitalVeterinarioService().save(new HospitalVeterinario()));
        rejected(() -> new Etapa5Service().consultarIdadeAnimal(0));

        SwingUtilities.invokeAndWait(() -> {
            JTable table = new JTable(new DefaultTableModel(new Object[][]{{"Zulu",2},{"Alpha",1}},new Object[]{"nome","id"}));
            TableFilterPanel panel = new TableFilterPanel(table);
            JTextField search = find(panel,JTextField.class).get(0);
            search.setText("alpha");
            require(table.getRowCount()==1 && table.convertRowIndexToModel(0)==1,"Busca deve selecionar a linha correta.");
            table.setModel(new DefaultTableModel(new Object[][]{{"Alpha novo",5},{"Zulu",6}},new Object[]{"nome","id"}));
            require(table.getRowCount()==1 && table.convertRowIndexToModel(0)==0,"Filtro deve continuar apos atualizar o modelo.");
            search.setText("["); require(table.getRowCount()==0,"Busca literal aceita caracteres de regex.");
            search.setText(""); require(table.getRowCount()==2,"Limpar restaura linhas.");
            table.getRowSorter().toggleSortOrder(0);
            require("Alpha novo".equals(table.getValueAt(0,0)),"Ordenacao da tabela.");
        });
        try (CachedRowSet rowset = RowSetProvider.newFactory().createCachedRowSet()) {
            RowSetMetaDataImpl meta = new RowSetMetaDataImpl();
            meta.setColumnCount(1); meta.setColumnName(1,"nome"); meta.setColumnLabel(1,"nome"); meta.setColumnType(1,Types.VARCHAR);
            rowset.setMetaData(meta); rowset.moveToInsertRow(); rowset.updateString(1,"Exemplo");
            rowset.insertRow(); rowset.moveToCurrentRow(); rowset.beforeFirst();
            DefaultTableModel model = ResultSetTableMapper.toTableModel(rowset);
            require(model.getRowCount()==1 && !model.isCellEditable(0,0),"Edicao deve ocorrer pelos formularios.");
        }
        System.out.println("Interface e validacoes sem banco: OK");
    }
    private static <T> List<T> find(Container parent,Class<T> type) {
        List<T> result=new ArrayList<>();
        for(Component c:parent.getComponents()) {
            if(type.isInstance(c)) result.add(type.cast(c));
            if(c instanceof Container nested)result.addAll(find(nested,type));
        }
        return result;
    }
    private static void rejected(Runnable action) {
        try { action.run(); } catch(ValidationException expected) { return; }
        throw new IllegalStateException("A validacao deveria rejeitar dados invalidos antes de acessar o banco.");
    }
    private static void require(boolean value,String message) { if(!value) throw new IllegalStateException(message); }
}
