package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

// ml-server/batch/description_similarity.py의 insert_pairs()가 항상 created_at을 채워서 적재하므로
// 이 컬럼은 실질적으로 항상 값이 있다. 엔티티의 nullable=false와 스키마를 맞춘다.
// band_similarity_new/band_similarity_old는 배치가 RENAME TABLE로만 다루고 매 실행마다
// band_similarity를 LIKE로 재생성하므로 이 제약도 함께 복제된다.
public class V51__set_band_similarity_created_at_not_null
        extends BaseJavaMigration {

    private static final String TABLE_NAME = "band_similarity";
    private static final String CREATED_AT_COLUMN = "created_at";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        String tableName = findTable(connection, TABLE_NAME);
        if (tableName == null) {
            return;
        }

        String createdAtColumn = findColumn(connection, tableName, CREATED_AT_COLUMN);
        if (createdAtColumn == null) {
            return;
        }

        boolean mysql = connection.getMetaData()
                .getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");

        execute(
                connection,
                "UPDATE " + quote(tableName, mysql)
                        + " SET " + quote(createdAtColumn, mysql)
                        + " = CURRENT_TIMESTAMP"
                        + " WHERE " + quote(createdAtColumn, mysql)
                        + " IS NULL"
        );

        if (mysql) {
            execute(
                    connection,
                    "ALTER TABLE " + quote(tableName, true)
                            + " MODIFY COLUMN "
                            + quote(createdAtColumn, true)
                            + " DATETIME NOT NULL"
            );
        } else {
            execute(
                    connection,
                    "ALTER TABLE " + quote(tableName, false)
                            + " ALTER COLUMN "
                            + quote(createdAtColumn, false)
                            + " SET NOT NULL"
            );
        }
    }

    private String findTable(
            Connection connection,
            String expectedName
    ) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();

        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(),
                connection.getSchema(),
                "%",
                new String[]{"TABLE"}
        )) {
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
                connection.getCatalog(),
                connection.getSchema(),
                tableName,
                "%"
        )) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");

                if (expectedName.equalsIgnoreCase(columnName)) {
                    return columnName;
                }
            }
        }

        return null;
    }

    private void execute(
            Connection connection,
            String sql
    ) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String quote(String identifier, boolean mysql) {
        return mysql ? "`" + identifier + "`" : "\"" + identifier + "\"";
    }
}
