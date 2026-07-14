package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V22__remove_legacy_session_profile_links extends BaseJavaMigration {

    private static final String LEGACY_TABLE = "session_profile_links";
    private static final String CURRENT_TABLE = "session_application_links";
    private static final String APPLICATION_TABLE = "session_applications";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase().contains("mysql");

        String legacyTable = findTable(connection, LEGACY_TABLE);
        if (legacyTable == null) return;

        String currentTable = requireTable(connection, CURRENT_TABLE);
        String applicationTable = requireTable(connection, APPLICATION_TABLE);
        requireColumns(connection, legacyTable,
                "created_at", "updated_at", "deleted_at", "url", "session_profile_id");
        requireColumns(connection, currentTable,
                "created_at", "updated_at", "deleted_at", "url", "session_application_id");
        requireColumns(connection, applicationTable, "session_application_id");

        long unmappableCount = queryCount(connection, """
                SELECT COUNT(*)
                FROM %s legacy
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM %s application
                    WHERE application.%s = legacy.%s
                )
                """.formatted(
                q(legacyTable, mysql), q(applicationTable, mysql),
                q("session_application_id", mysql), q("session_profile_id", mysql)));
        if (unmappableCount > 0) {
            throw new SQLException("Cannot remove legacy session profile links: "
                    + unmappableCount + " row(s) have no matching session application");
        }

        execute(connection, """
                INSERT INTO %s (%s, %s, %s, %s, %s)
                SELECT legacy.%s, legacy.%s, legacy.%s, legacy.%s, legacy.%s
                FROM %s legacy
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM %s current_link
                    WHERE current_link.%s = legacy.%s
                      AND current_link.%s = legacy.%s
                )
                """.formatted(
                q(currentTable, mysql),
                q("created_at", mysql), q("updated_at", mysql), q("deleted_at", mysql),
                q("url", mysql), q("session_application_id", mysql),
                q("created_at", mysql), q("updated_at", mysql), q("deleted_at", mysql),
                q("url", mysql), q("session_profile_id", mysql),
                q(legacyTable, mysql), q(currentTable, mysql),
                q("session_application_id", mysql), q("session_profile_id", mysql),
                q("url", mysql), q("url", mysql)));

        execute(connection, "DROP TABLE " + q(legacyTable, mysql));
    }

    private String requireTable(Connection connection, String tableName) throws SQLException {
        String actualName = findTable(connection, tableName);
        if (actualName == null) {
            throw new SQLException("Required table does not exist: " + tableName);
        }
        return actualName;
    }

    private void requireColumns(
            Connection connection,
            String tableName,
            String... columnNames
    ) throws SQLException {
        for (String columnName : columnNames) {
            if (!columnExists(connection, tableName, columnName)) {
                throw new SQLException("Required column does not exist: "
                        + tableName + "." + columnName);
            }
        }
    }

    private String findTable(Connection connection, String expectedName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (expectedName.equalsIgnoreCase(tableName)) return tableName;
            }
        }
        return null;
    }

    private boolean columnExists(
            Connection connection,
            String tableName,
            String expectedColumn
    ) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(
                connection.getCatalog(), connection.getSchema(), tableName, "%")) {
            while (columns.next()) {
                if (expectedColumn.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return true;
            }
        }
        return false;
    }

    private long queryCount(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String q(String identifier, boolean mysql) {
        return mysql
                ? "`" + identifier + "`"
                : "\"" + identifier.toUpperCase() + "\"";
    }
}
