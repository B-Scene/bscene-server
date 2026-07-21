package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;

public class V5__force_remove_recruitment_profile_column extends BaseJavaMigration {

    private static final String TABLE = "session_recruitment";
    private static final String COLUMN = "session_profile_id";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");

        if (mysql) {
            migrateMySql(connection);
        } else {
            migrateUsingMetadata(connection);
        }
    }

    private void migrateMySql(Connection connection) throws Exception {
        if (!mysqlColumnExists(connection)) {
            return;
        }

        for (String foreignKey : findMySqlForeignKeys(connection)) {
            execute(connection,
                    "ALTER TABLE `" + TABLE + "` DROP FOREIGN KEY `" + foreignKey + "`");
        }

        execute(connection,
                "ALTER TABLE `" + TABLE + "` DROP COLUMN `" + COLUMN + "`");
    }

    private boolean mysqlColumnExists(Connection connection) throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TABLE);
            statement.setString(2, COLUMN);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getLong(1) > 0;
            }
        }
    }

    private Set<String> findMySqlForeignKeys(Connection connection) throws Exception {
        Set<String> foreignKeys = new LinkedHashSet<>();
        String sql = """
                SELECT CONSTRAINT_NAME
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                  AND REFERENCED_TABLE_NAME IS NOT NULL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TABLE);
            statement.setString(2, COLUMN);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    foreignKeys.add(result.getString("CONSTRAINT_NAME"));
                }
            }
        }
        return foreignKeys;
    }

    private void migrateUsingMetadata(Connection connection) throws Exception {
        String tableName = findTable(connection);
        if (tableName == null) {
            return;
        }

        String columnName = findColumn(connection, tableName);
        if (columnName == null) {
            return;
        }

        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet keys = metadata.getImportedKeys(
                connection.getCatalog(), connection.getSchema(), tableName)) {
            while (keys.next()) {
                if (columnName.equalsIgnoreCase(keys.getString("FKCOLUMN_NAME"))) {
                    String foreignKeyName = keys.getString("FK_NAME");
                    if (foreignKeyName != null) {
                        execute(connection,
                                "ALTER TABLE \"" + tableName + "\" DROP CONSTRAINT \""
                                        + foreignKeyName + "\"");
                    }
                }
            }
        }

        execute(connection,
                "ALTER TABLE \"" + tableName + "\" DROP COLUMN \"" + columnName + "\"");
    }

    private String findTable(Connection connection) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (TABLE.equalsIgnoreCase(tableName)) {
                    return tableName;
                }
            }
        }
        return null;
    }

    private String findColumn(Connection connection, String tableName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(
                connection.getCatalog(), connection.getSchema(), tableName, "%")) {
            while (columns.next()) {
                if (COLUMN.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return columns.getString("COLUMN_NAME");
                }
            }
        }
        return null;
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
