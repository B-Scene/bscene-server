package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class V21__add_fan_profile_image extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String table = findTable(connection, "fan_profile", "FanProfile");
        if (table == null || findColumn(connection, table, "profile_image_url") != null) return;

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase().contains("mysql");
        String sql = "ALTER TABLE " + q(table, mysql)
                + " ADD COLUMN " + q("profile_image_url", mysql) + " VARCHAR(2048) NULL";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String findTable(Connection connection, String... expectedNames) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String actual = tables.getString("TABLE_NAME");
                for (String expected : expectedNames) {
                    if (expected.equalsIgnoreCase(actual)) return actual;
                }
            }
        }
        return null;
    }

    private String findColumn(Connection connection, String table, String expected) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), connection.getSchema(), table, "%")) {
            while (columns.next()) {
                if (expected.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return columns.getString("COLUMN_NAME");
                }
            }
        }
        return null;
    }

    private String q(String value, boolean mysql) {
        return mysql ? "`" + value + "`" : "\"" + value + "\"";
    }
}
