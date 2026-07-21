package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V14__add_stream_member_unique_constraint extends BaseJavaMigration {

    private static final String CONSTRAINT_NAME = "uk_stream_member_user_stream";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");

        String table = findTable(connection, "stream_member");
        if (table == null) {
            return;
        }

        String idColumn = findColumn(connection, table, "stream_member_id");
        String userColumn = findColumn(connection, table, "user_id");
        String streamColumn = findColumn(connection, table, "audio_stream_id");
        if (idColumn == null || userColumn == null || streamColumn == null) {
            return;
        }

        if (hasIndex(connection, table, CONSTRAINT_NAME)) {
            return;
        }

        // 제약 추가 전에 (user_id, audio_stream_id) 중복 행을 가장 오래된 행만 남기고 정리
        String dedupSql = "DELETE FROM " + quote(table, mysql)
                + " WHERE " + quote(idColumn, mysql) + " NOT IN ("
                + "SELECT keep_id FROM ("
                + "SELECT MIN(" + quote(idColumn, mysql) + ") AS keep_id FROM " + quote(table, mysql)
                + " GROUP BY " + quote(userColumn, mysql) + ", " + quote(streamColumn, mysql)
                + ") AS keeper)";

        // 동시 공동 진행자 교체 요청이 같은 (user, stream) 조합을 중복 삽입하지 못하도록 unique 제약 추가
        String constraintSql = "ALTER TABLE " + quote(table, mysql)
                + " ADD CONSTRAINT " + quote(CONSTRAINT_NAME, mysql)
                + " UNIQUE (" + quote(userColumn, mysql) + ", " + quote(streamColumn, mysql) + ")";

        try (Statement statement = connection.createStatement()) {
            statement.execute(dedupSql);
            statement.execute(constraintSql);
        }
    }

    private boolean hasIndex(Connection connection, String tableName, String indexName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(
                connection.getCatalog(), connection.getSchema(), tableName, false, false)) {
            while (indexes.next()) {
                String name = indexes.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
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
