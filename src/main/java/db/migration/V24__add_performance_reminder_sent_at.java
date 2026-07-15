package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class V24__add_performance_reminder_sent_at extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String table = findTable(
                connection,
                "PerformanceParticipation",
                "performance_participation"
        );

        if (table == null || columnExists(connection, table, "reminderSentAt")) {
            return;
        }

        boolean mysql = connection.getMetaData()
                .getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");

        String sql = "ALTER TABLE " + quote(table, mysql)
                + " ADD COLUMN " + quote("reminderSentAt", mysql)
                + " TIMESTAMP NULL";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String findTable(
            Connection connection,
            String... expectedNames
    ) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(),
                connection.getSchema(),
                "%",
                new String[]{"TABLE"}
        )) {
            while (tables.next()) {
                String actualName = tables.getString("TABLE_NAME");

                for (String expectedName : expectedNames) {
                    if (expectedName.equalsIgnoreCase(actualName)) {
                        return actualName;
                    }
                }
            }
        }

        return null;
    }

    private boolean columnExists(
            Connection connection,
            String table,
            String expectedColumn
    ) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(),
                connection.getSchema(),
                table,
                "%"
        )) {
            while (columns.next()) {
                if (expectedColumn.equalsIgnoreCase(
                        columns.getString("COLUMN_NAME")
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private String quote(String value, boolean mysql) {
        return mysql ? "`" + value + "`" : "\"" + value + "\"";
    }
}