package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

public class V57__add_apple_credentials extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql")) {
            return;
        }
        String userTable = findUserTable(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute(credentialTableSql(userTable));
            statement.execute(attemptTableSql());
        }
    }

    private String credentialTableSql(String userTable) {
        return "CREATE TABLE IF NOT EXISTS `apple_credentials` ("
                + "`subject_hash` VARCHAR(64) NOT NULL PRIMARY KEY, `version` BIGINT NOT NULL DEFAULT 0, "
                + "`provider_uid` VARCHAR(255) NULL, "
                + "`encrypted_refresh_token` TEXT NULL, `user_id` BIGINT NULL, "
                + "`signup_jti` VARCHAR(36) NULL, `signup_expires_at` DATETIME(6) NULL, "
                + "`login_expires_at` DATETIME(6) NULL, "
                + "`state` ENUM('CLEARED','PENDING_SIGNUP','ACTIVE','REVOKE_PENDING','REVOKE_FAILED') NOT NULL, "
                + "`revoke_attempts` INT NOT NULL DEFAULT 0, `next_attempt_at` DATETIME(6) NULL, "
                + "`last_failure` VARCHAR(64) NULL, `created_at` DATETIME(6) NOT NULL, "
                + "`updated_at` DATETIME(6) NOT NULL, "
                + "UNIQUE KEY `uk_apple_credentials_signup_jti` (`signup_jti`), "
                + "KEY `idx_apple_credentials_revoke` (`state`, `next_attempt_at`), "
                + "KEY `idx_apple_credentials_expiry` (`state`, `signup_expires_at`), "
                + "CONSTRAINT `fk_apple_credentials_user` FOREIGN KEY (`user_id`) REFERENCES "
                + quote(userTable) + " (`id`) ON DELETE SET NULL)";
    }

    private String attemptTableSql() {
        return "CREATE TABLE IF NOT EXISTS `apple_credential_attempts` ("
                + "`id` VARCHAR(36) NOT NULL PRIMARY KEY, `encrypted_refresh_token` TEXT NOT NULL, "
                + "`encrypted_identity_token` TEXT NOT NULL, `trusted_subject_hash` VARCHAR(64) NULL, "
                + "`expires_at` DATETIME(6) NOT NULL, `recovery_attempts` INT NOT NULL DEFAULT 0, "
                + "`next_recovery_at` DATETIME(6) NULL, "
                + "`state` ENUM('STAGED','REVIEW') NOT NULL, "
                + "`created_at` DATETIME(6) NOT NULL, `updated_at` DATETIME(6) NOT NULL, "
                + "KEY `idx_apple_credential_attempts_expiry` (`state`, `expires_at`), "
                + "KEY `idx_apple_credential_attempts_recovery` (`state`, `next_recovery_at`))";
    }

    private String findUserTable(Connection connection) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String table = tables.getString("TABLE_NAME");
                if ("user".equalsIgnoreCase(table)) {
                    return table;
                }
            }
        }
        throw new IllegalStateException("Apple 자격정보와 연결할 회원 테이블을 찾을 수 없습니다.");
    }

    private String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
}
