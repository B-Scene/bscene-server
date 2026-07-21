package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V10__remove_immutable_session_profile_fields extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String tableName = findTable(connection, "session_basic_profiles");
        if (tableName == null) {
            return;
        }

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");
        dropColumnIfExists(connection, tableName, "name", mysql);
        dropColumnIfExists(connection, tableName, "phone", mysql);
    }

    private void dropColumnIfExists(
            Connection connection,
            String tableName,
            String expectedColumn,
            boolean mysql
    ) throws Exception {
        String columnName = findColumn(connection, tableName, expectedColumn);
        if (columnName == null) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE " + quote(tableName, mysql)
                            + " DROP COLUMN " + quote(columnName, mysql)
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
            String expectedName
    ) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(
                connection.getCatalog(), connection.getSchema(), tableName, "%")) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                if (expectedName.equalsIgnoreCase(columnName)) {
                    return columnName;
                }
            }
        }
        return null;
    }

    private String quote(String identifier, boolean mysql) {
        return mysql ? "`" + identifier + "`" : "\"" + identifier + "\"";
    }
}
