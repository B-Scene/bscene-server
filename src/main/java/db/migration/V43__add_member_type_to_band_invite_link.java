package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V43__add_member_type_to_band_invite_link
        extends BaseJavaMigration {

    private static final String TABLE_NAME = "band_invite_link";
    private static final String BAND_ID_COLUMN = "band_id";
    private static final String MEMBER_TYPE_COLUMN = "member_type";

    private static final String OLD_UNIQUE_NAME =
            "uk_band_invite_link_band";

    private static final String NEW_UNIQUE_NAME =
            "uk_band_invite_link_band_type";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        String tableName = findTable(
                connection,
                TABLE_NAME
        );

        if (tableName == null) {
            return;
        }

        String bandIdColumn = findColumn(
                connection,
                tableName,
                BAND_ID_COLUMN
        );

        if (bandIdColumn == null) {
            return;
        }

        boolean mysql = connection.getMetaData()
                .getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");

        String memberTypeColumn = findColumn(
                connection,
                tableName,
                MEMBER_TYPE_COLUMN
        );

        if (memberTypeColumn == null) {
            execute(
                    connection,
                    "ALTER TABLE " + quote(tableName, mysql)
                            + " ADD COLUMN "
                            + quote(MEMBER_TYPE_COLUMN, mysql)
                            + " VARCHAR(20)"
            );

            memberTypeColumn = MEMBER_TYPE_COLUMN;
        }

        execute(
                connection,
                "UPDATE " + quote(tableName, mysql)
                        + " SET " + quote(memberTypeColumn, mysql)
                        + " = 'MEMBER'"
                        + " WHERE " + quote(memberTypeColumn, mysql)
                        + " IS NULL"
        );

        if (mysql) {
            execute(
                    connection,
                    "ALTER TABLE " + quote(tableName, true)
                            + " MODIFY COLUMN "
                            + quote(memberTypeColumn, true)
                            + " VARCHAR(20) NOT NULL"
            );
        } else {
            execute(
                    connection,
                    "ALTER TABLE " + quote(tableName, false)
                            + " ALTER COLUMN "
                            + quote(memberTypeColumn, false)
                            + " SET NOT NULL"
            );
        }

        String newUniqueIndex = findUniqueIndex(
                connection,
                tableName,
                NEW_UNIQUE_NAME
        );

        if (newUniqueIndex == null) {
            execute(
                    connection,
                    "ALTER TABLE " + quote(tableName, mysql)
                            + " ADD CONSTRAINT "
                            + quote(NEW_UNIQUE_NAME, mysql)
                            + " UNIQUE ("
                            + quote(bandIdColumn, mysql)
                            + ", "
                            + quote(memberTypeColumn, mysql)
                            + ")"
            );
        }

        String oldUniqueIndex = findUniqueIndex(
                connection,
                tableName,
                OLD_UNIQUE_NAME
        );

        if (oldUniqueIndex != null) {
            String dropSql = mysql
                    ? "ALTER TABLE " + quote(tableName, true)
                    + " DROP INDEX "
                    + quote(oldUniqueIndex, true)
                    : "ALTER TABLE " + quote(tableName, false)
                    + " DROP CONSTRAINT "
                    + quote(OLD_UNIQUE_NAME, false);

            execute(connection, dropSql);
        }
    }

    private String findTable(
            Connection connection,
            String expectedName
    ) throws Exception {
        DatabaseMetaData metadata =
                connection.getMetaData();

        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(),
                connection.getSchema(),
                "%",
                new String[]{"TABLE"}
        )) {
            while (tables.next()) {
                String tableName =
                        tables.getString("TABLE_NAME");

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
        DatabaseMetaData metadata =
                connection.getMetaData();

        try (ResultSet columns = metadata.getColumns(
                connection.getCatalog(),
                connection.getSchema(),
                tableName,
                "%"
        )) {
            while (columns.next()) {
                String columnName =
                        columns.getString("COLUMN_NAME");

                if (expectedName.equalsIgnoreCase(columnName)) {
                    return columnName;
                }
            }
        }

        return null;
    }

    private String findUniqueIndex(
            Connection connection,
            String tableName,
            String expectedName
    ) throws Exception {
        DatabaseMetaData metadata =
                connection.getMetaData();

        try (ResultSet indexes = metadata.getIndexInfo(
                connection.getCatalog(),
                connection.getSchema(),
                tableName,
                true,
                false
        )) {
            while (indexes.next()) {
                String indexName =
                        indexes.getString("INDEX_NAME");

                if (indexName != null
                        && expectedName.equalsIgnoreCase(indexName)) {
                    return indexName;
                }
            }
        }

        return null;
    }

    private void execute(
            Connection connection,
            String sql
    ) throws Exception {
        try (Statement statement =
                     connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String quote(
            String identifier,
            boolean mysql
    ) {
        return mysql
                ? "`" + identifier + "`"
                : "\"" + identifier + "\"";
    }
}
