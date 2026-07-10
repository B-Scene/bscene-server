package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V1__migrate_session_profile_to_application extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String productName = connection.getMetaData().getDatabaseProductName();
        boolean mysql = productName.toLowerCase().contains("mysql");

        renameTable(connection, mysql, "session_profiles", "session_applications");
        renameTable(connection, mysql, "session_profile_links", "session_application_links");

        renameColumn(connection, mysql, "session_applications",
                "session_profile_id", "session_application_id");
        renameColumn(connection, mysql, "session_application_links",
                "session_profile_link_id", "session_application_link_id");
        renameColumn(connection, mysql, "session_application_links",
                "session_profile_id", "session_application_id");
        renameColumn(connection, mysql, "BandMember",
                "session_profile_id", "session_application_id");

        dropColumnWithForeignKey(connection, mysql,
                "session_recruitment", "session_profile_id");

        createSubmissionTable(connection, mysql);
    }

    private void renameTable(
            Connection connection,
            boolean mysql,
            String oldName,
            String newName
    ) throws SQLException {
        String actualOldName = findTable(connection, oldName);
        if (actualOldName == null || findTable(connection, newName) != null) {
            return;
        }

        String sql = mysql
                ? "RENAME TABLE " + quote(actualOldName, true) + " TO " + quote(newName, true)
                : "ALTER TABLE " + quote(actualOldName, false) + " RENAME TO " + quote(newName, false);
        execute(connection, sql);
    }

    private void renameColumn(
            Connection connection,
            boolean mysql,
            String tableName,
            String oldName,
            String newName
    ) throws SQLException {
        String actualTableName = findTable(connection, tableName);
        if (actualTableName == null) {
            return;
        }

        String actualOldName = findColumn(connection, actualTableName, oldName);
        if (actualOldName == null || findColumn(connection, actualTableName, newName) != null) {
            return;
        }

        execute(connection,
                "ALTER TABLE " + quote(actualTableName, mysql)
                        + " RENAME COLUMN " + quote(actualOldName, mysql)
                        + " TO " + quote(newName, mysql));
    }

    private void dropColumnWithForeignKey(
            Connection connection,
            boolean mysql,
            String tableName,
            String columnName
    ) throws SQLException {
        String actualTableName = findTable(connection, tableName);
        if (actualTableName == null) {
            return;
        }

        String actualColumnName = findColumn(connection, actualTableName, columnName);
        if (actualColumnName == null) {
            return;
        }

        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet keys = metadata.getImportedKeys(
                connection.getCatalog(), connection.getSchema(), actualTableName)) {
            while (keys.next()) {
                if (actualColumnName.equalsIgnoreCase(keys.getString("FKCOLUMN_NAME"))) {
                    String foreignKeyName = keys.getString("FK_NAME");
                    if (foreignKeyName != null) {
                        String dropConstraint = mysql ? " DROP FOREIGN KEY " : " DROP CONSTRAINT ";
                        execute(connection,
                                "ALTER TABLE " + quote(actualTableName, mysql)
                                        + dropConstraint + quote(foreignKeyName, mysql));
                    }
                    break;
                }
            }
        }

        execute(connection,
                "ALTER TABLE " + quote(actualTableName, mysql)
                        + " DROP COLUMN " + quote(actualColumnName, mysql));
    }

    private void createSubmissionTable(Connection connection, boolean mysql) throws SQLException {
        if (!mysql
                || findTable(connection, "session_applications") == null
                || findTable(connection, "session_recruitment") == null
                || findTable(connection, "application_submissions") != null) {
            return;
        }

        execute(connection, """
                CREATE TABLE application_submissions (
                    application_submission_id BIGINT NOT NULL AUTO_INCREMENT,
                    session_recruitment_id BIGINT NOT NULL,
                    session_application_id BIGINT NOT NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (application_submission_id),
                    CONSTRAINT uk_application_submission_recruitment_application
                        UNIQUE (session_recruitment_id, session_application_id),
                    CONSTRAINT fk_application_submission_recruitment
                        FOREIGN KEY (session_recruitment_id)
                        REFERENCES session_recruitment (session_recruitment_id),
                    CONSTRAINT fk_application_submission_application
                        FOREIGN KEY (session_application_id)
                        REFERENCES session_applications (session_application_id)
                ) ENGINE=InnoDB
                """);
    }

    private String findTable(Connection connection, String expectedName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (expectedName.equalsIgnoreCase(tableName)) {
                    return tableName;
                }
            }
        }
        return null;
    }

    private String findColumn(
            Connection connection,
            String tableName,
            String expectedColumn
    ) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(
                connection.getCatalog(), connection.getSchema(), tableName, "%")) {
            while (columns.next()) {
                if (expectedColumn.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return columns.getString("COLUMN_NAME");
                }
            }
        }
        return null;
    }

    private String quote(String identifier, boolean mysql) {
        return mysql ? "`" + identifier + "`" : "\"" + identifier + "\"";
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
