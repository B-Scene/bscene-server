package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V46__add_stream_member_path extends BaseJavaMigration {

    private static final String CONSTRAINT_NAME = "uk_stream_member_path";

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

        // Flyway가 Hibernate(ddl-auto:update)보다 먼저 실행되므로, unique 인덱스 생성 전에 컬럼을 직접 추가
        String pathColumn = findColumn(connection, table, "path");
        if (pathColumn == null) {
            String addColumnSql = "ALTER TABLE " + quote(table, mysql)
                    + " ADD COLUMN " + quote("path", mysql) + " VARCHAR(64) NULL";
            try (Statement statement = connection.createStatement()) {
                statement.execute(addColumnSql);
            }
            pathColumn = "path";
        }

        if (!hasIndex(connection, table, CONSTRAINT_NAME)) {
            // 진행자 개인 송출 path의 중복 발급 방지
            String constraintSql = "ALTER TABLE " + quote(table, mysql)
                    + " ADD CONSTRAINT " + quote(CONSTRAINT_NAME, mysql)
                    + " UNIQUE (" + quote(pathColumn, mysql) + ")";

            try (Statement statement = connection.createStatement()) {
                statement.execute(constraintSql);
            }
        }

        backfillBroadcasterMembers(connection, table, mysql);
    }

    /*
     * 송출자를 StreamMember(ACCEPTED)로 등록하기 전에 생성된 구 데이터 백필.
     * enterRoom의 송출 path 발급이 StreamMember 행을 전제하므로, 종료되지 않은 라이브에 한해 채워 넣는다
     */
    private void backfillBroadcasterMembers(Connection connection, String memberTable, boolean mysql) throws Exception {
        String streamTable = findTable(connection, "audio_stream");
        if (streamTable == null) {
            return;
        }

        String sql = "INSERT INTO " + quote(memberTable, mysql)
                + " (" + quote("user_id", mysql) + ", " + quote("audio_stream_id", mysql) + ", "
                + quote("status", mysql) + ", " + quote("created_at", mysql) + ", " + quote("updated_at", mysql) + ")"
                + " SELECT a." + quote("broadcaster_id", mysql) + ", a." + quote("audio_stream_id", mysql)
                + ", 'ACCEPTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP"
                + " FROM " + quote(streamTable, mysql) + " a"
                + " WHERE a." + quote("status", mysql) + " IN ('SCHEDULED', 'OPEN')"
                + " AND NOT EXISTS (SELECT 1 FROM " + quote(memberTable, mysql) + " sm"
                + " WHERE sm." + quote("audio_stream_id", mysql) + " = a." + quote("audio_stream_id", mysql)
                + " AND sm." + quote("user_id", mysql) + " = a." + quote("broadcaster_id", mysql) + ")";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
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
