package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

public class V39__expand_session_application_portfolio_url extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String table = findTable(connection, "session_application_links");
        if (table == null) return;

        String column = findColumn(connection, table, "url");
        if (column == null) return;

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase(Locale.ROOT).contains("mysql");
        String sql = mysql
                ? "ALTER TABLE " + q(table, true) + " MODIFY COLUMN "
                        + q(column, true) + " TEXT NOT NULL"
                : "ALTER TABLE " + q(table, false) + " ALTER COLUMN "
                        + q(column, false) + " CLOB";
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

    private String q(String value, boolean mysql) {
        return mysql ? "`" + value.replace("`", "``") + "`"
                : "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
