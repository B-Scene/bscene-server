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

public class V4__remove_legacy_recruitment_profile_column extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String tableName = findTable(connection, "session_recruitment");
        if (tableName == null) {
            return;
        }

        String columnName = findColumn(connection, tableName, "session_profile_id");
        if (columnName == null) {
            return;
        }

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");

        for (String foreignKeyName : findForeignKeys(
                connection,
                tableName,
                columnName,
                mysql
        )) {
            execute(connection,
                    "ALTER TABLE " + quote(tableName, mysql)
                            + (mysql ? " DROP FOREIGN KEY " : " DROP CONSTRAINT ")
                            + quote(foreignKeyName, mysql));
        }

        execute(connection,
                "ALTER TABLE " + quote(tableName, mysql)
                        + " DROP COLUMN " + quote(columnName, mysql));
    }

    private Set<String> findForeignKeys(
            Connection connection,
            String tableName,
            String columnName,
            boolean mysql
    ) throws Exception {
        Set<String> foreignKeys = new LinkedHashSet<>();

        if (mysql) {
            String sql = """
                    SELECT CONSTRAINT_NAME
                    FROM information_schema.KEY_COLUMN_USAGE
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND LOWER(TABLE_NAME) = LOWER(?)
                      AND LOWER(COLUMN_NAME) = LOWER(?)
                      AND REFERENCED_TABLE_NAME IS NOT NULL
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, tableName);
                statement.setString(2, columnName);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        foreignKeys.add(rows.getString("CONSTRAINT_NAME"));
                    }
                }
            }
            return foreignKeys;
        }

        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet keys = metadata.getImportedKeys(
                connection.getCatalog(), connection.getSchema(), tableName)) {
            while (keys.next()) {
                if (columnName.equalsIgnoreCase(keys.getString("FKCOLUMN_NAME"))) {
                    String foreignKeyName = keys.getString("FK_NAME");
                    if (foreignKeyName != null) {
                        foreignKeys.add(foreignKeyName);
                    }
                }
            }
        }
        return foreignKeys;
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

    private void execute(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
