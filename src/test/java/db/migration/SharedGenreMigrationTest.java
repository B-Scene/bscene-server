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

class SharedGenreMigrationTest {

    private static final String URL =
            "jdbc:h2:mem:onboarding-genre-migration;MODE=MySQL;DB_CLOSE_DELAY=-1";

    @Test
    void migratesBandAndUserGenresAndRemovesCollapsedDuplicates() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE Band (
                        id BIGINT PRIMARY KEY,
                        genre VARCHAR(30) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE UserGenres (
                        id BIGINT PRIMARY KEY,
                        userId BIGINT NOT NULL,
                        genre VARCHAR(30) NOT NULL,
                        CONSTRAINT uk_user_genre UNIQUE (userId, genre)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE Performance (
                        id BIGINT PRIMARY KEY,
                        genre VARCHAR(30) NOT NULL
                    )
                    """);
            statement.execute("""
                    INSERT INTO Band VALUES
                        (1, 'INDIE_POP'), (2, 'FOLK'), (3, 'ROCK'), (4, 'METAL')
                    """);
            statement.execute("""
                    INSERT INTO UserGenres VALUES
                        (1, 10, 'ROCK'), (2, 10, 'RNB'), (3, 10, 'BLUES'),
                        (4, 20, 'PUNK'), (5, 20, 'ACOUSTIC')
                    """);
            statement.execute("""
                    INSERT INTO Performance VALUES
                        (1, 'PUNK'), (2, 'RNB'), (3, 'JAZZ')
                    """);
        }

        Flyway.configure()
                .dataSource(URL, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("31")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(URL, "sa", "")) {
            assertThat(genres(connection, "Band", "id"))
                    .containsExactly("INDIE", "FOLK_ROCK", "ETC", "METAL");
            assertThat(genres(connection, "Performance", "id"))
                    .containsExactly("PUNK_ROCK", "ETC", "JAZZ");
            assertThat(userGenres(connection))
                    .containsExactly("10:ETC", "10:BLUES", "20:PUNK_ROCK", "20:ETC");
        }
    }

    private List<String> genres(Connection connection, String table, String idColumn)
            throws Exception {
        List<String> genres = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT genre FROM " + table + " ORDER BY " + idColumn)) {
            while (resultSet.next()) genres.add(resultSet.getString(1));
        }
        return genres;
    }

    private List<String> userGenres(Connection connection) throws Exception {
        List<String> genres = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT userId, genre FROM UserGenres ORDER BY id")) {
            while (resultSet.next()) {
                genres.add(resultSet.getLong(1) + ":" + resultSet.getString(2));
            }
        }
        return genres;
    }
}
