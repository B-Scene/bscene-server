package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

// 전화번호 수집 중단(#448)에 따라 user.phone 컬럼을 삭제한다.
// SMS 인증 제거(#446) 이후 전화번호를 쓰는 기능이 없어 기존 값도 함께 삭제한다. (복구 불가)
// phone 에 걸린 unique 인덱스는 단일 컬럼 인덱스라 MySQL 이 컬럼과 함께 삭제한다.
public class V58__drop_user_phone extends BaseJavaMigration {

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

        String table = findTable(connection, "user");
        if (table == null || !hasColumn(connection, table, "phone")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE `" + table + "` DROP COLUMN `phone`");
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

    private boolean hasColumn(
            Connection connection,
            String table,
            String expectedName
    ) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(),
                connection.getSchema(),
                table,
                "%"
        )) {
            while (columns.next()) {
                if (expectedName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }

        return false;
    }
}
