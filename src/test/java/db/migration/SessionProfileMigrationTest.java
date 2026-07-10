package db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class SessionProfileMigrationTest {

    private static final String URL =
            "jdbc:h2:mem:legacy-migration;MODE=MySQL;DB_CLOSE_DELAY=-1";

    @Test
    void migrateLegacySessionProfileSchemaWithoutDataLoss() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE session_profiles (
                        session_profile_id BIGINT PRIMARY KEY,
                        user_id BIGINT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE session_profile_links (
                        session_profile_link_id BIGINT PRIMARY KEY,
                        session_profile_id BIGINT NOT NULL,
                        CONSTRAINT fk_legacy_link_profile FOREIGN KEY (session_profile_id)
                            REFERENCES session_profiles(session_profile_id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE BandMember (
                        id BIGINT PRIMARY KEY,
                        session_profile_id BIGINT,
                        CONSTRAINT fk_legacy_member_profile FOREIGN KEY (session_profile_id)
                            REFERENCES session_profiles(session_profile_id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE session_recruitment (
                        session_recruitment_id BIGINT PRIMARY KEY,
                        session_profile_id BIGINT NOT NULL,
                        CONSTRAINT fk_legacy_recruitment_profile FOREIGN KEY (session_profile_id)
                            REFERENCES session_profiles(session_profile_id)
                    )
                    """);
            statement.execute("INSERT INTO session_profiles VALUES (1, 10)");
            statement.execute("INSERT INTO session_profile_links VALUES (1, 1)");
            statement.execute("INSERT INTO BandMember VALUES (1, 1)");
            statement.execute("INSERT INTO session_recruitment VALUES (1, 1)");
        }

        Flyway.configure()
                .dataSource(URL, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(URL, "sa", "")) {
            assertThat(tableExists(connection, "session_applications")).isTrue();
            assertThat(tableExists(connection, "session_application_links")).isTrue();
            assertThat(tableExists(connection, "session_profiles")).isFalse();
            assertThat(columnExists(connection,
                    "session_applications", "session_application_id")).isTrue();
            assertThat(columnExists(connection,
                    "session_applications", "title")).isTrue();
            assertThat(columnExists(connection,
                    "session_applications", "purpose")).isTrue();
            assertThat(columnExists(connection,
                    "session_application_links", "session_application_id")).isTrue();
            assertThat(columnExists(connection,
                    "BandMember", "session_application_id")).isTrue();
            assertThat(columnExists(connection,
                    "session_recruitment", "session_profile_id")).isFalse();

            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT \"title\", \"purpose\" FROM \"session_applications\" "
                                 + "WHERE \"session_application_id\" = 1")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("기본 지원서");
                assertThat(rows.getString(2)).isEqualTo("기본");
            }
        }
    }

    @Test
    void backfillBlankPurposeFromExistingApplicationTable() throws Exception {
        String url = "jdbc:h2:mem:blank-purpose-migration;MODE=MySQL;DB_CLOSE_DELAY=-1";

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE session_applications (
                        session_application_id BIGINT PRIMARY KEY,
                        title VARCHAR(30) NOT NULL,
                        purpose VARCHAR(20) NOT NULL
                    )
                    """);
            statement.execute("""
                    INSERT INTO session_applications
                        (session_application_id, title, purpose)
                    VALUES (1, '기본 지원서', '')
                    """);
        }

        Flyway.configure()
                .dataSource(url, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("2")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery(
                     "SELECT purpose FROM session_applications WHERE session_application_id = 1")) {
            assertThat(row.next()).isTrue();
            assertThat(row.getString(1)).isEqualTo("기본");
        }
    }

    private boolean tableExists(Connection connection, String expectedName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (expectedName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(
            Connection connection,
            String expectedTable,
            String expectedColumn
    ) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(
                connection.getCatalog(), connection.getSchema(), "%", "%")) {
            while (columns.next()) {
                if (expectedTable.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                        && expectedColumn.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
