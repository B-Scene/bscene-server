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

public class V25__replace_band_member_session_application_with_member_type extends BaseJavaMigration {

    private static final String BAND_MEMBER_TABLE = "BandMember";
    private static final String SESSION_APPLICATION_COLUMN = "session_application_id";
    private static final String MEMBER_TYPE_COLUMN = "member_type";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase().contains("mysql");

        String bandMemberTable = findTable(connection, BAND_MEMBER_TABLE);
        if (bandMemberTable == null) return;

        if (!columnExists(connection, bandMemberTable, MEMBER_TYPE_COLUMN)) {
            execute(connection, "ALTER TABLE " + q(bandMemberTable, mysql)
                    + " ADD COLUMN " + q(MEMBER_TYPE_COLUMN, mysql)
                    + " VARCHAR(20) NOT NULL DEFAULT 'MEMBER'");
        }

        if (columnExists(connection, bandMemberTable, SESSION_APPLICATION_COLUMN)) {
            execute(connection, "UPDATE " + q(bandMemberTable, mysql)
                    + " SET " + q(MEMBER_TYPE_COLUMN, mysql) + " = 'SESSION'"
                    + " WHERE " + q(SESSION_APPLICATION_COLUMN, mysql) + " IS NOT NULL");

            dropForeignKeysForColumn(connection, mysql, bandMemberTable, SESSION_APPLICATION_COLUMN);
            execute(connection, "ALTER TABLE " + q(bandMemberTable, mysql)
                    + " DROP COLUMN " + q(SESSION_APPLICATION_COLUMN, mysql));
        }
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
