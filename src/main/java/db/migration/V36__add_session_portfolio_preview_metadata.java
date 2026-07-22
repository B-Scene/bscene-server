package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

public class V36__add_session_portfolio_preview_metadata extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String table = findTable(connection, "session_application_links");
        if (table == null) return;

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase(Locale.ROOT).contains("mysql");
        addColumnIfMissing(connection, table, "title", "VARCHAR(255)", mysql);
        addColumnIfMissing(connection, table, "thumbnail_url", "TEXT", mysql);
        addColumnIfMissing(connection, table, "media_type", "VARCHAR(30)", mysql);
    }

    private void addColumnIfMissing(
            Connection connection,
            String table,
            String column,
            String definition,
            boolean mysql
    ) throws Exception {
        if (findColumn(connection, table, column) != null) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + quote(table, mysql)
                    + " ADD COLUMN " + quote(column, mysql) + " " + definition);
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

    private String findColumn(Connection connection, String table, String expected)
            throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), connection.getSchema(), table, "%")) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME");
                if (expected.equalsIgnoreCase(name)) return name;
            }
        }
        return null;
    }

    private String quote(String value, boolean mysql) {
        return mysql ? "`" + value.replace("`", "``") + "`"
                : "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
