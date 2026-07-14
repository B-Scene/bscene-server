package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;

public class V23__remove_legacy_session_profiles extends BaseJavaMigration {

    private static final String LEGACY_TABLE = "session_profiles";
    private static final String CURRENT_TABLE = "session_applications";
    private static final String BAND_MEMBER_TABLE = "band_member";
    private static final String LEGACY_ID = "session_profile_id";
    private static final String CURRENT_ID = "session_application_id";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase().contains("mysql");

        String legacyTable = findTable(connection, LEGACY_TABLE);
        if (legacyTable == null) return;

        String currentTable = requireTable(connection, CURRENT_TABLE);
        requireColumn(connection, legacyTable, LEGACY_ID);
        requireColumn(connection, currentTable, CURRENT_ID);

        long unmappableProfiles = queryCount(connection, """
                SELECT COUNT(*)
                FROM %s legacy
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM %s application
                    WHERE application.%s = legacy.%s
                )
                """.formatted(
                q(legacyTable, mysql), q(currentTable, mysql),
                q(CURRENT_ID, mysql), q(LEGACY_ID, mysql)));
        if (unmappableProfiles > 0) {
            throw new SQLException("Cannot remove legacy session profiles: "
                    + unmappableProfiles + " profile(s) have no matching session application");
        }

        String bandMemberTable = findTable(connection, BAND_MEMBER_TABLE);
        if (bandMemberTable != null && columnExists(connection, bandMemberTable, LEGACY_ID)) {
            migrateBandMemberReferences(connection, mysql, bandMemberTable, currentTable);
            dropForeignKeysForColumn(connection, mysql, bandMemberTable, LEGACY_ID);
            execute(connection, "ALTER TABLE " + q(bandMemberTable, mysql)
                    + " DROP COLUMN " + q(LEGACY_ID, mysql));
        }

        Set<String> remainingReferences = findReferencingForeignKeys(connection, legacyTable);
        if (!remainingReferences.isEmpty()) {
            throw new SQLException("Cannot remove legacy session profiles; remaining foreign keys: "
                    + String.join(", ", remainingReferences));
        }

        execute(connection, "DROP TABLE " + q(legacyTable, mysql));
    }

    private void migrateBandMemberReferences(
            Connection connection,
            boolean mysql,
            String bandMemberTable,
            String currentTable
    ) throws SQLException {
        requireColumn(connection, bandMemberTable, CURRENT_ID);

        long conflictingReferences = queryCount(connection, """
                SELECT COUNT(*)
                FROM %s member
                WHERE member.%s IS NOT NULL
                  AND member.%s IS NOT NULL
                  AND member.%s <> member.%s
                """.formatted(
                q(bandMemberTable, mysql), q(LEGACY_ID, mysql), q(CURRENT_ID, mysql),
                q(LEGACY_ID, mysql), q(CURRENT_ID, mysql)));
        if (conflictingReferences > 0) {
            throw new SQLException("Cannot remove legacy session profiles: "
                    + conflictingReferences + " band member reference(s) conflict");
        }

        long unmappableReferences = queryCount(connection, """
                SELECT COUNT(*)
                FROM %s member
                WHERE member.%s IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM %s application
                      WHERE application.%s = member.%s
                  )
                """.formatted(
                q(bandMemberTable, mysql), q(LEGACY_ID, mysql),
                q(currentTable, mysql), q(CURRENT_ID, mysql), q(LEGACY_ID, mysql)));
        if (unmappableReferences > 0) {
            throw new SQLException("Cannot remove legacy session profiles: "
                    + unmappableReferences + " band member reference(s) cannot be mapped");
        }

        execute(connection, """
                UPDATE %s
                SET %s = %s
                WHERE %s IS NOT NULL
                  AND %s IS NULL
                """.formatted(
                q(bandMemberTable, mysql), q(CURRENT_ID, mysql), q(LEGACY_ID, mysql),
                q(LEGACY_ID, mysql), q(CURRENT_ID, mysql)));
    }

    private void dropForeignKeysForColumn(
            Connection connection,
            boolean mysql,
            String tableName,
            String columnName
    ) throws SQLException {
        Set<String> foreignKeyNames = new LinkedHashSet<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet keys = metadata.getImportedKeys(
                connection.getCatalog(), connection.getSchema(), tableName)) {
            while (keys.next()) {
                if (columnName.equalsIgnoreCase(keys.getString("FKCOLUMN_NAME"))) {
                    foreignKeyNames.add(keys.getString("FK_NAME"));
                }
            }
        }
        for (String foreignKeyName : foreignKeyNames) {
            String operation = mysql ? " DROP FOREIGN KEY " : " DROP CONSTRAINT ";
            execute(connection, "ALTER TABLE " + q(tableName, mysql)
                    + operation + q(foreignKeyName, mysql));
        }
    }

    private Set<String> findReferencingForeignKeys(
            Connection connection,
            String tableName
    ) throws SQLException {
        Set<String> references = new LinkedHashSet<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet keys = metadata.getExportedKeys(
                connection.getCatalog(), connection.getSchema(), tableName)) {
            while (keys.next()) {
                references.add(keys.getString("FKTABLE_NAME") + "." + keys.getString("FK_NAME"));
            }
        }
        return references;
    }

    private String requireTable(Connection connection, String tableName) throws SQLException {
        String actualName = findTable(connection, tableName);
        if (actualName == null) throw new SQLException("Required table does not exist: " + tableName);
        return actualName;
    }

    private void requireColumn(
            Connection connection,
            String tableName,
            String columnName
    ) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            throw new SQLException("Required column does not exist: "
                    + tableName + "." + columnName);
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
