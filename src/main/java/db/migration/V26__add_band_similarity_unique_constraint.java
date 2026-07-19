package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

// ml-server/batch/description_similarity.py 배치가 CREATE TABLE ... LIKE band_similarity로
// 스테이징 테이블을 만든 뒤 RENAME TABLE로 스왑하므로, 이 제약이 있어야 스왑 후에도
// (band_id, similar_band_id) 중복이 재생성되지 않는다. 배치를 처음 실행하기 전에
// 이 마이그레이션이 먼저 적용되어 있어야 한다 (Spring 앱을 한 번 기동하면 자동 적용됨).
public class V26__add_band_similarity_unique_constraint extends BaseJavaMigration {

    private static final String CONSTRAINT_NAME = "uk_band_similarity_band_similar_band";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");

        String table = findTable(connection, "band_similarity");
        if (table == null) {
            return;
        }

        String idColumn = findColumn(connection, table, "id");
        String bandColumn = findColumn(connection, table, "band_id");
        String similarBandColumn = findColumn(connection, table, "similar_band_id");
        if (idColumn == null || bandColumn == null || similarBandColumn == null) {
            return;
        }

        if (hasIndex(connection, table, CONSTRAINT_NAME)) {
            return;
        }

        // 제약 추가 전에 (band_id, similar_band_id) 중복 행을 가장 오래된 행만 남기고 정리
        String dedupSql = "DELETE FROM " + quote(table, mysql)
                + " WHERE " + quote(idColumn, mysql) + " NOT IN ("
                + "SELECT keep_id FROM ("
                + "SELECT MIN(" + quote(idColumn, mysql) + ") AS keep_id FROM " + quote(table, mysql)
                + " GROUP BY " + quote(bandColumn, mysql) + ", " + quote(similarBandColumn, mysql)
                + ") AS keeper)";

        String constraintSql = "ALTER TABLE " + quote(table, mysql)
                + " ADD CONSTRAINT " + quote(CONSTRAINT_NAME, mysql)
                + " UNIQUE (" + quote(bandColumn, mysql) + ", " + quote(similarBandColumn, mysql) + ")";

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
