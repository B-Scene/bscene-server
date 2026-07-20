package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

public class V30__add_etc_to_session_recruitment_part extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase(Locale.ROOT).contains("mysql");
        if (!mysql) return;

        String table = findTable(connection, "session_recruitment");
        String column = table == null ? null : findColumn(connection, table, "part");
        if (column == null) return;

        String columnType = findMysqlColumnType(connection, table, column);
        if (columnType == null || !columnType.toLowerCase(Locale.ROOT).startsWith("enum(")) return;
        if (columnType.toUpperCase(Locale.ROOT).contains("'ETC'")) return;

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + q(table)
                    + " MODIFY COLUMN " + q(column)
                    + " ENUM('BASS','DRUM','ETC','GUITAR','KEYBOARD','VOCAL') NOT NULL");
        }
    }

    private String findMysqlColumnType(Connection connection, String table, String column)
            throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM " + q(table)
                     + " LIKE '" + column.replace("'", "''") + "'")) {
            return columns.next() ? columns.getString("Type") : null;
        }
    }

    private String findTable(Connection connection, String expected) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME");
                if (expected.equalsIgnoreCase(name)) return name;
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
