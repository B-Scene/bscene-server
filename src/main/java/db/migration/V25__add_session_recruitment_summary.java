package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class V25__add_session_recruitment_summary extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String table = findTable(connection, "session_recruitment");
        if (table == null || findColumn(connection, table, "summary") != null) return;

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase().contains("mysql");
        String contentColumn = findColumn(connection, table, "content");
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + q(table, mysql)
                    + " ADD COLUMN " + q("summary", mysql) + " VARCHAR(50)");
            String backfillExpression = contentColumn == null
                    ? "''"
                    : "SUBSTRING(COALESCE(" + q(contentColumn, mysql) + ", ''), 1, 50)";
            statement.executeUpdate("UPDATE " + q(table, mysql)
                    + " SET " + q("summary", mysql) + " = " + backfillExpression);
            if (mysql) {
                statement.execute("ALTER TABLE " + q(table, true)
                        + " MODIFY COLUMN " + q("summary", true) + " VARCHAR(50) NOT NULL");
            } else {
                statement.execute("ALTER TABLE " + q(table, false)
                        + " ALTER COLUMN " + q("summary", false) + " SET NOT NULL");
            }
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

    private String findColumn(Connection connection, String table, String expected) throws Exception {
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
        return mysql ? "`" + value + "`" : "\"" + value + "\"";
    }
}
