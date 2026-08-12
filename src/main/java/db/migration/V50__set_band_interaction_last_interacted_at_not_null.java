package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

// upsertInteraction()이 INSERT 시 last_interacted_at을 항상 채우므로(BandInteractionRepository)
// 이 컬럼은 실질적으로 항상 값이 있다. 엔티티의 nullable=false와 스키마를 맞춘다.
public class V50__set_band_interaction_last_interacted_at_not_null
        extends BaseJavaMigration {

    private static final String TABLE_NAME = "band_interaction";
    private static final String CREATED_AT_COLUMN = "created_at";
    private static final String LAST_INTERACTED_AT_COLUMN = "last_interacted_at";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        String tableName = findTable(connection, TABLE_NAME);
        if (tableName == null) {
            return;
        }

        String lastInteractedAtColumn = findColumn(connection, tableName, LAST_INTERACTED_AT_COLUMN);
        if (lastInteractedAtColumn == null) {
            return;
        }

        String createdAtColumn = findColumn(connection, tableName, CREATED_AT_COLUMN);

        boolean mysql = connection.getMetaData()
                .getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");

        if (createdAtColumn != null) {
            execute(
                    connection,
                    "UPDATE " + quote(tableName, mysql)
                            + " SET " + quote(lastInteractedAtColumn, mysql)
                            + " = " + quote(createdAtColumn, mysql)
                            + " WHERE " + quote(lastInteractedAtColumn, mysql)
                            + " IS NULL"
            );
        }

        if (mysql) {
            execute(
                    connection,
                    "ALTER TABLE " + quote(tableName, true)
                            + " MODIFY COLUMN "
                            + quote(lastInteractedAtColumn, true)
                            + " DATETIME NOT NULL"
            );
        } else {
            execute(
                    connection,
                    "ALTER TABLE " + quote(tableName, false)
                            + " ALTER COLUMN "
                            + quote(lastInteractedAtColumn, false)
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
