package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V11__add_application_submission_checked_at extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String tableName = findTable(connection, "application_submissions");
        if (tableName == null || columnExists(connection, tableName, "checked_at")) {
            return;
        }
        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase().contains("mysql");
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + quote(tableName, mysql)
                    + " ADD COLUMN " + quote("checked_at", mysql) + " TIMESTAMP NULL");
        }
    }

    private String findTable(Connection connection, String expectedName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME");
                if (expectedName.equalsIgnoreCase(name)) return name;
            }
        }
        return null;
    }

    private boolean columnExists(Connection connection, String table, String expected) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), connection.getSchema(), table, "%")) {
            while (columns.next()) {
                if (expected.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return true;
            }
        }
        return false;
    }

    private String quote(String identifier, boolean mysql) {
        return mysql ? "`" + identifier + "`" : "\"" + identifier + "\"";
    }
}
