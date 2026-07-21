package db.migration;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

public class V33__limit_session_recruitment_text_lengths extends BaseJavaMigration {

    private static final int MAX_LENGTH = 50;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String table = findTable(connection, "session_recruitment");
        if (table == null) return;

        limitColumn(connection, table, "recruitment_title");
        limitColumn(connection, table, "practice_schedule");
        limitColumn(connection, table, "practice_place");
    }

    private void limitColumn(Connection connection, String table, String expectedColumn)
            throws Exception {
        ColumnMetadata column = findColumn(connection, table, expectedColumn);
        if (column == null) return;

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase(Locale.ROOT).contains("mysql");
        String quotedTable = q(table, mysql);
        String quotedColumn = q(column.name(), mysql);

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM " + quotedTable
                             + " WHERE CHAR_LENGTH(" + quotedColumn + ") > " + MAX_LENGTH)) {
            resultSet.next();
            if (resultSet.getLong(1) > 0) {
                throw new FlywayException(
                        table + "." + column.name()
                                + "에 50자를 초과한 기존 데이터가 있어 길이를 변경할 수 없습니다."
                );
            }
        }

        String alterSql = mysql
                ? "ALTER TABLE " + quotedTable + " MODIFY COLUMN " + quotedColumn
                        + " VARCHAR(" + MAX_LENGTH + ") "
                        + (column.nullable() ? "NULL" : "NOT NULL")
                : "ALTER TABLE " + quotedTable + " ALTER COLUMN " + quotedColumn
                        + " VARCHAR(" + MAX_LENGTH + ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(alterSql);
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

    private ColumnMetadata findColumn(Connection connection, String table, String expected)
            throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), connection.getSchema(), table, "%")) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME");
                if (expected.equalsIgnoreCase(name)) {
                    return new ColumnMetadata(
                            name,
                            "YES".equalsIgnoreCase(columns.getString("IS_NULLABLE"))
                    );
                }
            }
        }
        return null;
    }

    private String q(String value, boolean mysql) {
        return mysql ? "`" + value.replace("`", "``") + "`"
                : "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private record ColumnMetadata(String name, boolean nullable) {
    }
}
