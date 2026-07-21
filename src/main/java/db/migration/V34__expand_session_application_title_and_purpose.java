package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

public class V34__expand_session_application_title_and_purpose extends BaseJavaMigration {

    private static final int MAX_LENGTH = 50;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String table = findTable(connection, "session_applications");
        if (table == null) return;

        expandColumn(connection, table, "title");
        expandColumn(connection, table, "purpose");
    }

    private void expandColumn(Connection connection, String table, String expectedColumn)
            throws Exception {
        ColumnMetadata column = findColumn(connection, table, expectedColumn);
        if (column == null || column.length() >= MAX_LENGTH) return;

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase(Locale.ROOT).contains("mysql");
        String quotedTable = q(table, mysql);
        String quotedColumn = q(column.name(), mysql);
        String sql = mysql
                ? "ALTER TABLE " + quotedTable + " MODIFY COLUMN " + quotedColumn
                        + " VARCHAR(" + MAX_LENGTH + ") "
                        + (column.nullable() ? "NULL" : "NOT NULL")
                : "ALTER TABLE " + quotedTable + " ALTER COLUMN " + quotedColumn
                        + " VARCHAR(" + MAX_LENGTH + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String findTable(Connection connection, String expected) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME");
                if (expected.equalsIgnoreCase(name)) return name;
            }
        }
        return null;
    }

    private ColumnMetadata findColumn(Connection connection, String table, String expected)
            throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), connection.getSchema(), table, "%")) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME");
                if (expected.equalsIgnoreCase(name)) {
                    return new ColumnMetadata(
                            name,
                            columns.getInt("COLUMN_SIZE"),
                            "YES".equalsIgnoreCase(columns.getString("IS_NULLABLE"))
                    );
                }
            }
        }
        return null;
    }

    private String q(String value, boolean mysql) {
        return mysql ? "`" + value.replace("`", "``") + "`"
                : "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private record ColumnMetadata(String name, int length, boolean nullable) {
    }
}
