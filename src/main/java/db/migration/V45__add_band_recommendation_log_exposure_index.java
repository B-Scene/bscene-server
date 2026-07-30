package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

// 노출 피로도 감쇠 계산(findRecentVisibleExposures)이 매 추천 요청마다
// user_id + band_id + created_at 으로 조회하는데 인덱스가 없어
// 로그가 쌓일수록 스캔 범위가 선형 증가함.
// position 을 후행 컬럼으로 포함해 커버링 인덱스로 만든다.
//
// band_recommendation_log는 Flyway가 아니라 Hibernate ddl-auto가 만드는 테이블이라,
// 완전히 빈 DB에 처음 배포될 때는 Flyway가 Hibernate보다 먼저 돌기 때문에
// 이 시점에 테이블이 아직 없을 수 있다 (테스트의 @SpringBootTest에서 매번 재현됨).
// 그 경우에만 스킵한다 — 테이블이 있는데 컬럼명이 잘못됐다면 이 마이그레이션은 그대로 실패해야 한다.
public class V45__add_band_recommendation_log_exposure_index extends BaseJavaMigration {

    private static final String TABLE_NAME = "band_recommendation_log";
    private static final String INDEX_NAME = "idx_band_recommendation_log_user_band_created";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        if (findTable(connection, TABLE_NAME) == null) {
            return;
        }

        String sql = "CREATE INDEX " + INDEX_NAME
                + " ON " + TABLE_NAME
                + " (user_id, band_id, created_at, `position`)";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String findTable(Connection connection, String expectedName) throws Exception {
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
}
