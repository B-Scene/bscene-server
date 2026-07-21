package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

public class V34__add_unknown_to_region_values extends BaseJavaMigration {

    private static final String EXPANDED_REGIONS = """
            ENUM('SEOUL','GYEONGGI','INCHEON','BUSAN','DAEGU','GWANGJU','DAEJEON','ULSAN',\
            'SEJONG','CHUNGBUK','CHUNGNAM','JEONBUK','JEONNAM','GYEONGBUK','GYEONGNAM',\
            'GANGWON','JEJU','UNKNOWN')\
            """;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isMysql(connection)) return;

        migrateTable(connection, "region", "user_regions", "UserRegions");
        migrateTable(connection, "region", "band", "Band");
        migrateTable(connection, "region", "performance", "Performance");
        migrateTable(connection, "region", "session_recruitment");
        migrateTable(connection, "region", "session_applications");
    }

    private void migrateTable(Connection connection, String expectedColumn, String... expectedTables)
            throws Exception {
        String table = findTable(connection, expectedTables);
        String column = table == null ? null : findColumn(connection, table, expectedColumn);
        if (column == null) return;

        String columnType = findMysqlColumnType(connection, table, column);
        if (columnType == null || !columnType.toLowerCase(Locale.ROOT).startsWith("enum(")) return;
        if (columnType.toUpperCase(Locale.ROOT).contains("'UNKNOWN'")) return;

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + q(table)
                    + " MODIFY COLUMN " + q(column)
                    + " " + EXPANDED_REGIONS + " NOT NULL");
        }
    }

    private boolean isMysql(Connection connection) throws Exception {
        return connection.getMetaData().getDatabaseProductName()
                .toLowerCase(Locale.ROOT).contains("mysql");
    }

    private String findMysqlColumnType(Connection connection, String table, String column)
            throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM " + q(table)
                     + " LIKE '" + column.replace("'", "''") + "'")) {
            return columns.next() ? columns.getString("Type") : null;
        }
    }

    private String findTable(Connection connection, String... expectedNames) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME");
                for (String expected : expectedNames) {
                    if (expected.equalsIgnoreCase(name)) return name;
                }
            }
        }
        return null;
    }

    private String findColumn(Connection connection, String table, String expected) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), connection.getSchema(), table, "%")) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME");
                if (expected.equalsIgnoreCase(name)) return name;
            }
        }
        return null;
    }

    private String q(String value) {
        return "`" + value.replace("`", "``") + "`";
    }
}
