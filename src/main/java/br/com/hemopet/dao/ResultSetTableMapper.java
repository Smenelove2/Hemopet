package br.com.hemopet.dao;

import javax.swing.table.DefaultTableModel;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public final class ResultSetTableMapper {
    private ResultSetTableMapper() {
    }

    public static DefaultTableModel toTableModel(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        DefaultTableModel model = new DefaultTableModel();

        for (int column = 1; column <= columnCount; column++) {
            model.addColumn(metaData.getColumnLabel(column));
        }

        while (resultSet.next()) {
            Object[] row = new Object[columnCount];
            for (int column = 1; column <= columnCount; column++) {
                row[column - 1] = resultSet.getObject(column);
            }
            model.addRow(row);
        }

        return model;
    }
}
