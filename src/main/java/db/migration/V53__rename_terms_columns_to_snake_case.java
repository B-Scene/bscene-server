package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class V53__rename_terms_columns_to_snake_case extends BaseJavaMigration {

    // V49가 camelCase로 만든 컬럼을 Hibernate 기본 네이밍 전략(snake_case)에 맞춘다
    private static final Map<String, String> RENAMES = new LinkedHashMap<>();

    static {
        RENAMES.put("termId", "term_id");
        RENAMES.put("createdAt", "created_at");
        RENAMES.put("updatedAt", "updated_at");
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase(Locale.ROOT)
                .contains("mysql");

        String termsTable = findTable(connection, "terms", "Terms");
        if (termsTable == null) {
            return;
        }

        Map<String, String> pending = new LinkedHashMap<>();
        for (Map.Entry<String, String> rename : RENAMES.entrySet()) {
            String camel = findColumn(connection, termsTable, rename.getKey());
            boolean snakeExists = findColumn(connection, termsTable, rename.getValue()) != null;
            if (camel != null && !snakeExists) {
                pending.put(camel, rename.getValue());
            }
        }
        if (pending.isEmpty()) {
            return;
        }

        List<ForeignKey> foreignKeys = pending.containsKey("termId")
                ? dropReferencingForeignKeys(connection, termsTable, mysql)
                : List.of();

        for (Map.Entry<String, String> rename : pending.entrySet()) {
            renameColumn(connection, termsTable, rename.getKey(), rename.getValue(), mysql);
        }

        for (ForeignKey foreignKey : foreignKeys) {
            restoreForeignKey(connection, termsTable, foreignKey, mysql);
        }
    }

    private void renameColumn(
            Connection connection,
            String table,
            String from,
            String to,
            boolean mysql
    ) throws Exception {
        String sql = mysql
                ? "ALTER TABLE " + q(table, true) + " RENAME COLUMN "
                        + q(from, true) + " TO " + q(to, true)
                : "ALTER TABLE " + q(table, false) + " ALTER COLUMN "
                        + q(from, false) + " RENAME TO " + q(to, false);
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private List<ForeignKey> dropReferencingForeignKeys(
            Connection connection,
            String termsTable,
            boolean mysql
    ) throws Exception {
        List<ForeignKey> foreignKeys = new ArrayList<>();
        try (ResultSet keys = connection.getMetaData().getExportedKeys(
                connection.getCatalog(), connection.getSchema(), termsTable)) {
            while (keys.next()) {
                if ("termId".equalsIgnoreCase(keys.getString("PKCOLUMN_NAME"))) {
                    foreignKeys.add(new ForeignKey(
                            keys.getString("FKTABLE_NAME"),
                            keys.getString("FKCOLUMN_NAME"),
                            keys.getString("FK_NAME")
                    ));
                }
            }
        }

        for (ForeignKey foreignKey : foreignKeys) {
            String sql = "ALTER TABLE " + q(foreignKey.table(), mysql)
                    + " DROP " + (mysql ? "FOREIGN KEY " : "CONSTRAINT ")
                    + q(foreignKey.name(), mysql);
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
        return foreignKeys;
    }

    private void restoreForeignKey(
            Connection connection,
            String termsTable,
            ForeignKey foreignKey,
            boolean mysql
    ) throws Exception {
        String sql = "ALTER TABLE " + q(foreignKey.table(), mysql)
                + " ADD CONSTRAINT " + q(foreignKey.name(), mysql)
                + " FOREIGN KEY (" + q(foreignKey.column(), mysql) + ") REFERENCES "
                + q(termsTable, mysql) + " (" + q("term_id", mysql) + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String findTable(Connection connection, String... expectedNames) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME");
                for (String expected : expectedNames) {
                    if (expected.equalsIgnoreCase(name)) {
                        return name;
                    }
                }
            }
        }
        return null;
    }

    private String findColumn(Connection connection, String table, String expectedName)
            throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), connection.getSchema(), table, "%")) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME");
                if (expectedName.equals(name)) {
                    return name;
                }
            }
        }
        return null;
    }

    private String q(String value, boolean mysql) {
        return mysql ? "`" + value.replace("`", "``") + "`"
                : "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private record ForeignKey(String table, String column, String name) {
    }
}
