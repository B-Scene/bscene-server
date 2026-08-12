package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V52__add_requested_to_stream_member_status extends BaseJavaMigration {

    private static final String TABLE_NAME = "stream_member";
    private static final String STATUS_COLUMN = "status";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        boolean mysql = connection.getMetaData()
                .getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");

        if (!mysql) {
            return;
        }

        String tableName = findTable(connection, TABLE_NAME);
        if (tableName == null) {
            return;
        }

        String statusColumn = findColumn(connection, tableName, STATUS_COLUMN);
        if (statusColumn == null) {
            return;
        }

        String sql = "ALTER TABLE `" + tableName + "` "
                + "MODIFY COLUMN `" + statusColumn + "` "
                + "ENUM('INVITED','ACCEPTED','REJECTED','REQUESTED') NOT NULL";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
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
}
