package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V2__add_session_application_metadata extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");

        addRequiredColumn(connection, mysql,
                "title", 30, "기본 지원서");
        addRequiredColumn(connection, mysql,
                "purpose", 20, "기본");
    }

    private void addRequiredColumn(
            Connection connection,
            boolean mysql,
            String columnName,
            int length,
            String legacyValue
    ) throws SQLException {
        String tableName = findTable(connection, "session_applications");
        if (tableName == null || findColumn(connection, tableName, columnName) != null) {
            return;
        }

        execute(connection,
                "ALTER TABLE " + quote(tableName, mysql)
                        + " ADD COLUMN " + quote(columnName, mysql)
                        + " VARCHAR(" + length + ")");
        execute(connection,
                "UPDATE " + quote(tableName, mysql)
                        + " SET " + quote(columnName, mysql) + " = '" + legacyValue + "'"
                        + " WHERE " + quote(columnName, mysql) + " IS NULL");

        String makeNotNull = mysql
                ? "ALTER TABLE " + quote(tableName, true)
                        + " MODIFY COLUMN " + quote(columnName, true)
                        + " VARCHAR(" + length + ") NOT NULL"
                : "ALTER TABLE " + quote(tableName, false)
                        + " ALTER COLUMN " + quote(columnName, false) + " SET NOT NULL";
        execute(connection, makeNotNull);
    }

    private String findTable(Connection connection, String expectedName) throws SQLException {
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
    ) throws SQLException {
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

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
