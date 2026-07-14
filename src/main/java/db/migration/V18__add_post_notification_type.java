package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class V18__add_post_notification_type extends BaseJavaMigration {

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

        String table = findTable(connection, "Notifications");
        if (table == null) {
            return;
        }

        String sql = "ALTER TABLE `" + table + "` "
                + "MODIFY COLUMN `type` "
                + "ENUM('PERFORMANCE','LIVE','MESSAGE','SESSION','POST') NOT NULL";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String findTable(
            Connection connection,
            String expectedName
    ) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(
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
}