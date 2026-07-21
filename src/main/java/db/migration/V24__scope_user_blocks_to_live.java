package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** Changes user blocks from a global relationship to a live-room-scoped relationship. */
public class V24__scope_user_blocks_to_live extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String blockTable = findTable(connection, "user_blocks");
        String streamTable = findTable(connection, "audio_stream");
        if (blockTable == null || streamTable == null) return;

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase().contains("mysql");
        try (Statement statement = connection.createStatement()) {
            // Global rows cannot be assigned to a particular live safely.
            statement.executeUpdate("DELETE FROM " + q(blockTable, mysql));

            // MySQL may reuse the old composite unique index for the blocker FK.
            // Give both user FKs their own indexes before removing that unique index.
            if (mysql && !indexExists(connection, blockTable, "idx_user_blocks_blocker")) {
                statement.execute("CREATE INDEX " + q("idx_user_blocks_blocker", true)
                        + " ON " + q(blockTable, true) + " (" + q("blocker_id", true) + ")");
            }
            if (mysql && !indexExists(connection, blockTable, "idx_user_blocks_blocked")) {
                statement.execute("CREATE INDEX " + q("idx_user_blocks_blocked", true)
                        + " ON " + q(blockTable, true) + " (" + q("blocked_id", true) + ")");
            }

            if (mysql && indexExists(connection, blockTable, "uk_user_blocks_blocker_blocked")) {
                statement.execute("ALTER TABLE " + q(blockTable, true)
                        + " DROP INDEX " + q("uk_user_blocks_blocker_blocked", true));
            } else if (!mysql && indexExists(
                    connection, blockTable, "uk_user_blocks_blocker_blocked")) {
                statement.execute("ALTER TABLE " + q(blockTable, false)
                        + " DROP CONSTRAINT " + q("uk_user_blocks_blocker_blocked", false));
            }
            if (!columnExists(connection, blockTable, "audio_stream_id")) {
                statement.execute("ALTER TABLE " + q(blockTable, mysql)
                        + " ADD COLUMN " + q("audio_stream_id", mysql) + " BIGINT NOT NULL");
            }
            if (!foreignKeyExists(connection, blockTable, "fk_user_blocks_audio_stream")) {
                statement.execute("ALTER TABLE " + q(blockTable, mysql)
                        + " ADD CONSTRAINT " + q("fk_user_blocks_audio_stream", mysql)
                        + " FOREIGN KEY (" + q("audio_stream_id", mysql) + ") REFERENCES "
                        + q(streamTable, mysql) + " (" + q("audio_stream_id", mysql) + ")");
            }
            if (!indexExists(connection, blockTable, "uk_user_blocks_live_blocker_blocked")) {
                statement.execute("ALTER TABLE " + q(blockTable, mysql)
                        + " ADD CONSTRAINT " + q("uk_user_blocks_live_blocker_blocked", mysql)
                        + " UNIQUE (" + q("audio_stream_id", mysql) + ", "
                        + q("blocker_id", mysql) + ", " + q("blocked_id", mysql) + ")");
            }
        }
    }

    private boolean columnExists(Connection connection, String table, String expected)
            throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(), connection.getSchema(), table, "%")) {
            while (columns.next()) {
                if (expected.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return true;
            }
        }
        return false;
    }

    private boolean indexExists(Connection connection, String table, String expected)
            throws Exception {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(
                connection.getCatalog(), connection.getSchema(), table, false, false)) {
            while (indexes.next()) {
                if (expected.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return true;
            }
        }
        return false;
    }

    private boolean foreignKeyExists(Connection connection, String table, String expected)
            throws Exception {
        try (ResultSet keys = connection.getMetaData().getImportedKeys(
                connection.getCatalog(), connection.getSchema(), table)) {
            while (keys.next()) {
                if (expected.equalsIgnoreCase(keys.getString("FK_NAME"))) return true;
            }
        }
        return false;
    }

    private String findTable(Connection connection, String expected) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (expected.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return tables.getString("TABLE_NAME");
                }
            }
        }
        return null;
    }

    private String q(String value, boolean mysql) {
        return mysql ? "`" + value + "`" : "\"" + value + "\"";
    }
}
