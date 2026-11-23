package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

// 모바일 Apple 로그인 대비 oauth_accounts 보강.
// 1) provider ENUM에 APPLE 추가 — Hibernate가 @Enumerated(STRING)을 MySQL 네이티브 ENUM으로 만들어
//    상수가 늘 때마다 마이그레이션이 필요하다 (V52, V55 선례). SocialProvider.APPLE 과 맞춘다.
// 2) (provider, provider_uid) unique 추가 — 같은 소셜 계정으로 동시 가입 시
//    saveAndFlush → DataIntegrityViolationException → OAUTH409_1 로 잡는 처리의 전제.
//    적용 전 중복 행 0건 확인 완료 (2026-11-05).
public class V56__add_apple_provider_and_unique_oauth_account extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = connection.getMetaData()
                .getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");

        if (!mysql) {
            return;
        }

        String table = findTable(connection, "oauth_accounts");
        if (table == null) {
            return;
        }

        String modifyProvider = "ALTER TABLE `" + table + "` "
                + "MODIFY COLUMN `provider` "
                + "ENUM('GOOGLE','KAKAO','APPLE') NOT NULL";

        String addUnique = "ALTER TABLE `" + table + "` "
                + "ADD CONSTRAINT `uk_oauth_accounts_provider_uid` "
                + "UNIQUE (`provider`, `provider_uid`)";

        try (Statement statement = connection.createStatement()) {
            statement.execute(modifyProvider);
            statement.execute(addUnique);
        }
    }

    private String findTable(
            Connection connection,
            String expectedName
    ) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(
                connection.getCatalog(),
                connection.getSchema(),
                "%",
                new String[]{"TABLE"}
        )) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (expectedName.equalsIgnoreCase(tableName)) {
                    return tableName;
                }
            }
        }

        return null;
    }
}
