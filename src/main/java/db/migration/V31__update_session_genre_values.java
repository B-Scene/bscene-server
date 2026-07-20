package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

public class V31__update_session_genre_values extends BaseJavaMigration {

    private static final String EXPANDED_GENRES = """
            ENUM('ROCK','INDIE_POP','JAZZ','METAL','FOLK','RNB','BLUES','PUNK','ACOUSTIC',\
            'PSYCHEDELIC_ROCK','ALTERNATIVE_ROCK','INDIE','ELECTRONIC_ROCK','POP','POP_ROCK',\
            'PUNK_ROCK','FOLK_ROCK','HARD_ROCK','ETC')\
            """;

    private static final String CURRENT_GENRES = """
            ENUM('METAL','BLUES','PSYCHEDELIC_ROCK','ALTERNATIVE_ROCK','INDIE',\
            'ELECTRONIC_ROCK','JAZZ','POP','POP_ROCK','PUNK_ROCK','FOLK_ROCK','HARD_ROCK','ETC')\
            """;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        migrateTable(connection, "session_applications");
        migrateTable(connection, "session_recruitment");
    }

    private void migrateTable(Connection connection, String expectedTable) throws Exception {
        String table = findTable(connection, expectedTable);
        String genreColumn = table == null ? null : findColumn(connection, table, "genre");
        if (genreColumn == null) return;

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase(Locale.ROOT).contains("mysql");
        boolean nativeEnum = mysql && isNativeEnum(connection, table, genreColumn);

        try (Statement statement = connection.createStatement()) {
            if (nativeEnum) {
                statement.execute("ALTER TABLE " + q(table, true)
                        + " MODIFY COLUMN " + q(genreColumn, true)
                        + " " + EXPANDED_GENRES + " NOT NULL");
            }

            statement.executeUpdate("UPDATE " + q(table, mysql)
                    + " SET " + q(genreColumn, mysql) + " = CASE "
                    + q(genreColumn, mysql)
                    + " WHEN 'INDIE_POP' THEN 'INDIE'"
                    + " WHEN 'FOLK' THEN 'FOLK_ROCK'"
                    + " WHEN 'PUNK' THEN 'PUNK_ROCK'"
                    + " WHEN 'ROCK' THEN 'ETC'"
                    + " WHEN 'RNB' THEN 'ETC'"
                    + " WHEN 'ACOUSTIC' THEN 'ETC'"
                    + " ELSE " + q(genreColumn, mysql) + " END"
                    + " WHERE " + q(genreColumn, mysql)
                    + " IN ('INDIE_POP','FOLK','PUNK','ROCK','RNB','ACOUSTIC')");

            if (nativeEnum) {
                statement.execute("ALTER TABLE " + q(table, true)
                        + " MODIFY COLUMN " + q(genreColumn, true)
                        + " " + CURRENT_GENRES + " NOT NULL");
            }
        }
    }

    private boolean isNativeEnum(Connection connection, String table, String column)
            throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM " + q(table, true)
                     + " LIKE '" + column.replace("'", "''") + "'")) {
            return columns.next()
                    && columns.getString("Type").toLowerCase(Locale.ROOT).startsWith("enum(");
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

    private String findColumn(Connection connection, String table, String expected)
            throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), connection.getSchema(), table, "%")) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME");
                if (expected.equalsIgnoreCase(name)) return name;
            }
        }
        return null;
    }

    private String q(String value, boolean mysql) {
        return mysql ? "`" + value.replace("`", "``") + "`"
                : "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
