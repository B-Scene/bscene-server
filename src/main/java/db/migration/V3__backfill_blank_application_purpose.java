package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V3__backfill_blank_application_purpose extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String tableName = findTable(connection, "session_applications");
        if (tableName == null) {
            return;
        }

        String purposeColumn = findColumn(connection, tableName, "purpose");
        if (purposeColumn == null) {
            return;
        }

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");
        String quotedTable = quote(tableName, mysql);
        String quotedPurpose = quote(purposeColumn, mysql);

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "UPDATE " + quotedTable
                            + " SET " + quotedPurpose + " = '기본'"
                            + " WHERE " + quotedPurpose + " IS NULL"
                            + " OR TRIM(" + quotedPurpose + ") = ''"
            );
        }
    }

    private String findTable(Connection connection, String expectedName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (expectedName.equalsIgnoreCase(tableName)) {
                    return tableName;
                }
            }
        }
        return null;
    }

    private String findColumn(
            Connection connection,
            String tableName,
            String expectedColumn
    ) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(
                connection.getCatalog(), connection.getSchema(), tableName, "%")) {
            while (columns.next()) {
                if (expectedColumn.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return columns.getString("COLUMN_NAME");
                }
            }
        }
        return null;
    }

    private String quote(String identifier, boolean mysql) {
        return mysql ? "`" + identifier + "`" : "\"" + identifier + "\"";
    }
}
