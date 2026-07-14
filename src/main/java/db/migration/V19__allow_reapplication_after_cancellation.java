package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V19__allow_reapplication_after_cancellation extends BaseJavaMigration {
    private static final String TABLE = "application_submissions";
    private static final String CONSTRAINT =
            "uk_application_submission_recruitment_application";
    private static final String RECRUITMENT_COLUMN = "session_recruitment_id";
    private static final String APPLICATION_COLUMN = "session_application_id";
    private static final String RECRUITMENT_INDEX =
            "idx_application_submissions_recruitment";
    private static final String APPLICATION_INDEX =
            "idx_application_submissions_application";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String tableName = findTable(connection, TABLE);
        if (tableName == null) return;

        String constraintName = findUniqueIndex(connection, tableName, CONSTRAINT);
        if (constraintName == null) return;

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase().contains("mysql");
        createIndexIfAbsent(
                connection, tableName, RECRUITMENT_INDEX, RECRUITMENT_COLUMN, mysql);
        createIndexIfAbsent(
                connection, tableName, APPLICATION_INDEX, APPLICATION_COLUMN, mysql);

        String sql = mysql
                ? "ALTER TABLE " + quote(tableName, true)
                    + " DROP INDEX " + quote(constraintName, true)
                : "ALTER TABLE " + quote(tableName, false)
                    + " DROP CONSTRAINT " + quote(constraintName, false);

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void createIndexIfAbsent(
            Connection connection,
            String tableName,
            String indexName,
            String columnName,
            boolean mysql
    ) throws Exception {
        if (findIndex(connection, tableName, indexName) != null) return;

        String sql = "CREATE INDEX " + quote(indexName, mysql)
                + " ON " + quote(tableName, mysql)
                + " (" + quote(columnName, mysql) + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String findTable(Connection connection, String expected) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME");
                if (expected.equalsIgnoreCase(name)) return name;
            }
        }
        return null;
    }

    private String findUniqueIndex(
            Connection connection,
            String tableName,
            String expected
    ) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(
                connection.getCatalog(), connection.getSchema(), tableName, true, false)) {
            while (indexes.next()) {
                String name = indexes.getString("INDEX_NAME");
                if (name != null && expected.equalsIgnoreCase(name)) return name;
            }
        }
        return null;
    }

    private String findIndex(
            Connection connection,
            String tableName,
            String expected
    ) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(
                connection.getCatalog(), connection.getSchema(), tableName, false, false)) {
            while (indexes.next()) {
                String name = indexes.getString("INDEX_NAME");
                if (name != null && expected.equalsIgnoreCase(name)) return name;
            }
        }
        return null;
    }

    private String quote(String value, boolean mysql) {
        return mysql ? "`" + value + "`" : "\"" + value + "\"";
    }
}
