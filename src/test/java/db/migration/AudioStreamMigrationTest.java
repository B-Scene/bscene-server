package db.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AudioStreamMigrationTest {

    @Test
    void addAndBackfillAudioStreamColumns() throws Exception {
        String url = "jdbc:h2:mem:audio-stream-migration;MODE=MySQL;DB_CLOSE_DELAY=-1";
        createLegacySchema(url, false);

        migrateFromVersion11(url);

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery(
                     "SELECT \"band_id\", \"closed_viewer_count\" FROM audio_stream "
                             + "WHERE audio_stream_id = 1")) {
            assertThat(row.next()).isTrue();
            assertThat(row.getLong(1)).isEqualTo(20L);
            assertThat(row.getObject(2)).isNull();
            assertThat(isNullable(connection, "audio_stream", "band_id")).isFalse();
        }
    }

    @Test
    void rejectAmbiguousBandBackfill() throws Exception {
        String url = "jdbc:h2:mem:ambiguous-audio-stream-migration;MODE=MySQL;DB_CLOSE_DELAY=-1";
        createLegacySchema(url, true);

        assertThrows(FlywayException.class, () -> migrateFromVersion11(url));
    }

    private void createLegacySchema(String url, boolean ambiguous) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE audio_stream (
                        audio_stream_id BIGINT PRIMARY KEY,
                        broadcaster_id BIGINT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE band_member_profile (
                        id BIGINT PRIMARY KEY,
                        active BOOLEAN NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE BandMember (
                        id BIGINT PRIMARY KEY,
                        userId BIGINT NOT NULL,
                        bandId BIGINT NOT NULL,
                        bandMemberProfileId BIGINT NOT NULL,
                        status VARCHAR(30) NOT NULL
                    )
                    """);
            statement.execute("INSERT INTO audio_stream VALUES (1, 10)");
            statement.execute("INSERT INTO band_member_profile VALUES (100, TRUE)");
            statement.execute("INSERT INTO BandMember VALUES (1000, 10, 20, 100, 'ACCEPTED')");
            if (ambiguous) {
                statement.execute("INSERT INTO band_member_profile VALUES (101, TRUE)");
                statement.execute("INSERT INTO BandMember VALUES (1001, 10, 21, 101, 'ACCEPTED')");
            }
        }
    }

    private void migrateFromVersion11(String url) {
        Flyway.configure()
                .dataSource(url, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("11")
                .load()
                .migrate();
    }

    // 테이블 이름을 JDBC 패턴으로 넘기면 H2가 대소문자를 구분해 매칭에 실패하므로,
    // 패턴은 %로 넓히고 자바에서 대소문자 무시 비교 (SessionProfileMigrationTest.columnExists와 동일한 방식)
    private boolean isNullable(Connection connection, String table, String column) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(
                connection.getCatalog(), connection.getSchema(), "%", "%")) {
            while (columns.next()) {
                if (table.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                        && column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return columns.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                }
            }
        }
        throw new IllegalStateException("Column not found: " + table + "." + column);
    }
}
