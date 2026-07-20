package db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionGenreMigrationTest {

    private static final String URL =
            "jdbc:h2:mem:session-genre-migration;MODE=MySQL;DB_CLOSE_DELAY=-1";

    @Test
    void migratesLegacySessionGenresWithoutDataLoss() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE session_applications (
                        session_application_id BIGINT PRIMARY KEY,
                        genre VARCHAR(30) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE session_recruitment (
                        session_recruitment_id BIGINT PRIMARY KEY,
                        genre VARCHAR(30) NOT NULL
                    )
                    """);
            statement.execute("""
                    INSERT INTO session_applications VALUES
                        (1, 'INDIE_POP'), (2, 'FOLK'), (3, 'PUNK'), (4, 'METAL')
                    """);
            statement.execute("""
                    INSERT INTO session_recruitment VALUES
                        (1, 'ROCK'), (2, 'RNB'), (3, 'ACOUSTIC'), (4, 'BLUES')
                    """);
        }

        Flyway.configure()
                .dataSource(URL, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("30")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(URL, "sa", "")) {
            assertThat(genres(connection, "session_applications"))
                    .containsExactly("INDIE", "FOLK_ROCK", "PUNK_ROCK", "METAL");
            assertThat(genres(connection, "session_recruitment"))
                    .containsExactly("ETC", "ETC", "ETC", "BLUES");
        }
    }

    private List<String> genres(Connection connection, String table) throws Exception {
        List<String> genres = new ArrayList<>();
        String idColumn = table.equals("session_applications")
                ? "session_application_id" : "session_recruitment_id";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT genre FROM " + table + " ORDER BY " + idColumn)) {
            while (resultSet.next()) {
                genres.add(resultSet.getString("genre"));
            }
        }
        return genres;
    }
}
