package db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class SessionRecruitmentTextLengthMigrationTest {

    private static final String URL =
            "jdbc:h2:mem:session-recruitment-text-length;MODE=MySQL;DB_CLOSE_DELAY=-1";

    @Test
    void limitsTitleScheduleAndPlaceToFiftyCharacters() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE session_recruitment (
                        session_recruitment_id BIGINT PRIMARY KEY,
                        recruitment_title VARCHAR(100) NOT NULL,
                        practice_schedule VARCHAR(100),
                        practice_place VARCHAR(100)
                    )
                    """);
            statement.execute("""
                    INSERT INTO session_recruitment VALUES
                        (1, '드럼 세션 모집', '매주 토요일 18시', '홍대 합주실')
                    """);
        }

        Flyway.configure()
                .dataSource(URL, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("32")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(URL, "sa", "")) {
            assertThat(columnSize(connection, "RECRUITMENT_TITLE")).isEqualTo(50);
            assertThat(columnSize(connection, "PRACTICE_SCHEDULE")).isEqualTo(50);
            assertThat(columnSize(connection, "PRACTICE_PLACE")).isEqualTo(50);
        }
    }

    private int columnSize(Connection connection, String column) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                null, null, "SESSION_RECRUITMENT", column)) {
            assertThat(columns.next()).isTrue();
            return columns.getInt("COLUMN_SIZE");
        }
    }
}
