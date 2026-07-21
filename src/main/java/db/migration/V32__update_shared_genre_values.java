package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class V32__update_shared_genre_values extends BaseJavaMigration {

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
        migrateSimpleTable(connection, "Band");
        migrateSimpleTable(connection, "Performance");
        migrateUserGenres(connection);
    }

    private void migrateSimpleTable(Connection connection, String expectedTable) throws Exception {
        String table = findTable(connection, expectedTable);
        String genreColumn = table == null ? null : findColumn(connection, table, "genre");
        if (genreColumn == null) return;

        boolean mysql = isMysql(connection);
        boolean nativeEnum = mysql && isNativeEnum(connection, table, genreColumn);
        expandEnumIfNeeded(connection, table, genreColumn, nativeEnum);

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(updateGenreSql(table, genreColumn, mysql));
        }

        shrinkEnumIfNeeded(connection, table, genreColumn, nativeEnum);
    }

    private void migrateUserGenres(Connection connection) throws Exception {
        String table = findTable(connection, "UserGenres");
        if (table == null) return;

        String idColumn = findColumn(connection, table, "id");
        String userIdColumn = findColumn(connection, table, "userId");
        String genreColumn = findColumn(connection, table, "genre");
        if (idColumn == null || userIdColumn == null || genreColumn == null) return;

        boolean mysql = isMysql(connection);
        boolean nativeEnum = mysql && isNativeEnum(connection, table, genreColumn);
        expandEnumIfNeeded(connection, table, genreColumn, nativeEnum);

        List<UserGenreRow> rows = readUserGenres(
                connection, table, idColumn, userIdColumn, genreColumn, mysql);
        Set<String> occupied = new HashSet<>();
        String updateSql = "UPDATE " + q(table, mysql) + " SET " + q(genreColumn, mysql)
                + " = ? WHERE " + q(idColumn, mysql) + " = ?";
        String deleteSql = "DELETE FROM " + q(table, mysql)
                + " WHERE " + q(idColumn, mysql) + " = ?";

        try (PreparedStatement update = connection.prepareStatement(updateSql);
             PreparedStatement delete = connection.prepareStatement(deleteSql)) {
            for (UserGenreRow row : rows) {
                String target = mapGenre(row.genre());
                String key = row.userId() + "\u0000" + target;
                if (!occupied.add(key)) {
                    delete.setLong(1, row.id());
                    delete.addBatch();
                } else if (!target.equals(row.genre())) {
                    update.setString(1, target);
                    update.setLong(2, row.id());
                    update.addBatch();
                }
            }
            delete.executeBatch();
            update.executeBatch();
        }

        shrinkEnumIfNeeded(connection, table, genreColumn, nativeEnum);
    }

    private List<UserGenreRow> readUserGenres(
            Connection connection,
            String table,
            String idColumn,
            String userIdColumn,
            String genreColumn,
            boolean mysql
    ) throws Exception {
        List<UserGenreRow> rows = new ArrayList<>();
        String sql = "SELECT " + q(idColumn, mysql) + ", " + q(userIdColumn, mysql)
                + ", " + q(genreColumn, mysql) + " FROM " + q(table, mysql)
                + " ORDER BY " + q(idColumn, mysql);
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                rows.add(new UserGenreRow(
                        resultSet.getLong(1),
                        resultSet.getLong(2),
                        resultSet.getString(3)
                ));
            }
        }
        return rows;
    }

    private String updateGenreSql(String table, String genreColumn, boolean mysql) {
        String column = q(genreColumn, mysql);
        return "UPDATE " + q(table, mysql) + " SET " + column + " = CASE " + column
                + " WHEN 'INDIE_POP' THEN 'INDIE'"
                + " WHEN 'FOLK' THEN 'FOLK_ROCK'"
                + " WHEN 'PUNK' THEN 'PUNK_ROCK'"
                + " WHEN 'ROCK' THEN 'ETC'"
                + " WHEN 'RNB' THEN 'ETC'"
                + " WHEN 'ACOUSTIC' THEN 'ETC'"
                + " ELSE " + column + " END"
                + " WHERE " + column
                + " IN ('INDIE_POP','FOLK','PUNK','ROCK','RNB','ACOUSTIC')";
    }

    private String mapGenre(String genre) {
        return switch (genre) {
            case "INDIE_POP" -> "INDIE";
            case "FOLK" -> "FOLK_ROCK";
            case "PUNK" -> "PUNK_ROCK";
            case "ROCK", "RNB", "ACOUSTIC" -> "ETC";
            default -> genre;
        };
    }

    private void expandEnumIfNeeded(
            Connection connection, String table, String column, boolean nativeEnum
    ) throws Exception {
        if (!nativeEnum) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + q(table, true)
                    + " MODIFY COLUMN " + q(column, true)
                    + " " + EXPANDED_GENRES + " NOT NULL");
        }
    }

    private void shrinkEnumIfNeeded(
            Connection connection, String table, String column, boolean nativeEnum
    ) throws Exception {
        if (!nativeEnum) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + q(table, true)
                    + " MODIFY COLUMN " + q(column, true)
                    + " " + CURRENT_GENRES + " NOT NULL");
        }
    }

    private boolean isMysql(Connection connection) throws Exception {
        return connection.getMetaData().getDatabaseProductName()
                .toLowerCase(Locale.ROOT).contains("mysql");
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

    private record UserGenreRow(long id, long userId, String genre) {
    }
}
