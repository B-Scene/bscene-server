package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

public class V13__add_audio_stream_band_and_closed_viewer_count extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = isMySql(connection);

        String audioStreamTable = findTable(connection, "audio_stream");
        if (audioStreamTable == null) {
            return;
        }

        String bandColumn = findColumn(connection, audioStreamTable, "band_id");
        if (bandColumn == null) {
            bandColumn = "band_id";
            execute(connection,
                    "ALTER TABLE " + quote(audioStreamTable, mysql)
                            + " ADD COLUMN " + quote(bandColumn, mysql) + " BIGINT");
        }

        if (findColumn(connection, audioStreamTable, "closed_viewer_count") == null) {
            execute(connection,
                    "ALTER TABLE " + quote(audioStreamTable, mysql)
                            + " ADD COLUMN " + quote("closed_viewer_count", mysql) + " INTEGER");
        }

        backfillBandId(connection, audioStreamTable, bandColumn, mysql);
        makeBandIdNotNull(connection, audioStreamTable, bandColumn, mysql);
    }

    private void backfillBandId(
            Connection connection,
            String audioStreamTable,
            String bandColumn,
            boolean mysql
    ) throws Exception {
        String audioStreamId = requireColumn(connection, audioStreamTable, "audio_stream_id");
        String broadcasterId = requireColumn(connection, audioStreamTable, "broadcaster_id");

        if (!hasNull(connection, audioStreamTable, bandColumn, mysql)) {
            return;
        }

        String bandMemberTable = findTable(connection, "band_member", "BandMember");
        String profileTable = findTable(connection, "band_member_profile", "BandMemberProfile");
        if (bandMemberTable == null || profileTable == null) {
            throw new SQLException("Cannot backfill audio_stream.band_id: band membership tables are missing");
        }

        String memberUserId = requireColumn(connection, bandMemberTable, "user_id", "userId");
        String memberBandId = requireColumn(connection, bandMemberTable, "band_id", "bandId");
        String memberProfileId = requireColumn(
                connection, bandMemberTable, "band_member_profile_id", "bandMemberProfileId");
        String memberStatus = requireColumn(connection, bandMemberTable, "status");
        String profileId = requireColumn(connection, profileTable, "id");
        String profileActive = requireColumn(connection, profileTable, "active");

        String candidates = "SELECT bm." + quote(memberUserId, mysql) + " AS broadcaster_id, "
                + "bm." + quote(memberBandId, mysql) + " AS band_id"
                + " FROM " + quote(bandMemberTable, mysql) + " bm"
                + " JOIN " + quote(profileTable, mysql) + " bmp"
                + " ON bmp." + quote(profileId, mysql)
                + " = bm." + quote(memberProfileId, mysql)
                + " WHERE bm." + quote(memberStatus, mysql) + " = 'ACCEPTED'"
                + " AND bmp." + quote(profileActive, mysql) + " = TRUE";

        String invalidSql = "SELECT a." + quote(audioStreamId, mysql)
                + ", COUNT(DISTINCT candidate.band_id)"
                + " FROM " + quote(audioStreamTable, mysql) + " a"
                + " LEFT JOIN (" + candidates + ") candidate"
                + " ON candidate.broadcaster_id = a." + quote(broadcasterId, mysql)
                + " WHERE a." + quote(bandColumn, mysql) + " IS NULL"
                + " GROUP BY a." + quote(audioStreamId, mysql)
                + " HAVING COUNT(DISTINCT candidate.band_id) <> 1";

        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(invalidSql)) {
            if (rows.next()) {
                throw new SQLException(
                        "Cannot safely backfill audio_stream.band_id for audio_stream_id="
                                + rows.getLong(1) + ": expected exactly one active accepted band, found "
                                + rows.getLong(2)
                );
            }
        }

        String updateTarget = mysql
                ? "a." + quote(bandColumn, true)
                : quote(bandColumn, false);
        String updateSql = "UPDATE " + quote(audioStreamTable, mysql) + " a"
                + " SET " + updateTarget + " = ("
                + "SELECT MIN(bm." + quote(memberBandId, mysql) + ")"
                + " FROM " + quote(bandMemberTable, mysql) + " bm"
                + " JOIN " + quote(profileTable, mysql) + " bmp"
                + " ON bmp." + quote(profileId, mysql)
                + " = bm." + quote(memberProfileId, mysql)
                + " WHERE bm." + quote(memberUserId, mysql)
                + " = a." + quote(broadcasterId, mysql)
                + " AND bm." + quote(memberStatus, mysql) + " = 'ACCEPTED'"
                + " AND bmp." + quote(profileActive, mysql) + " = TRUE)"
                + " WHERE a." + quote(bandColumn, mysql) + " IS NULL";
        execute(connection, updateSql);

        if (hasNull(connection, audioStreamTable, bandColumn, mysql)) {
            throw new SQLException("audio_stream.band_id backfill left unresolved rows");
        }
    }

    private void makeBandIdNotNull(
            Connection connection,
            String table,
            String column,
            boolean mysql
    ) throws Exception {
        if (!isNullable(connection, table, column)) {
            return;
        }

        String sql = mysql
                ? "ALTER TABLE " + quote(table, true)
                        + " MODIFY COLUMN " + quote(column, true) + " BIGINT NOT NULL"
                : "ALTER TABLE " + quote(table, false)
                        + " ALTER COLUMN " + quote(column, false) + " SET NOT NULL";
        execute(connection, sql);
    }

    private boolean hasNull(Connection connection, String table, String column, boolean mysql)
            throws Exception {
        String sql = "SELECT COUNT(*) FROM " + quote(table, mysql)
                + " WHERE " + quote(column, mysql) + " IS NULL";
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getLong(1) > 0;
        }
    }

    private boolean isNullable(Connection connection, String table, String column) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(
                connection.getCatalog(), connection.getSchema(), table, "%")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return columns.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                }
            }
        }
        throw new SQLException("Column not found: " + table + "." + column);
    }

    private String requireColumn(Connection connection, String table, String... expected) throws Exception {
        String column = findColumn(connection, table, expected);
        if (column == null) {
            throw new SQLException(
                    "Required migration column is missing: " + table + "." + Arrays.toString(expected));
        }
        return column;
    }

    private String findTable(Connection connection, String... expectedNames) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                for (String expectedName : expectedNames) {
                    if (expectedName.equalsIgnoreCase(tableName)) {
                        return tableName;
                    }
                }
            }
        }
        return null;
    }

    private String findColumn(Connection connection, String tableName, String... expectedNames)
            throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(
                connection.getCatalog(), connection.getSchema(), tableName, "%")) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                for (String expectedName : expectedNames) {
                    if (expectedName.equalsIgnoreCase(columnName)) {
                        return columnName;
                    }
                }
            }
        }
        return null;
    }

    private boolean isMySql(Connection connection) throws Exception {
        return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql");
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String quote(String identifier, boolean mysql) {
        return mysql ? "`" + identifier + "`" : "\"" + identifier + "\"";
    }
}
