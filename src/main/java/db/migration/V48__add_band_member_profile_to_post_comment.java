package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

// #365 밴드모드 댓글 : 댓글에 작성 명의(밴드 멤버 프로필) FK 컬럼 추가
// null = 팬모드 댓글, 값 있음 = 밴드모드 댓글 (작성 시점 멤버 프로필 박제)
public class V48__add_band_member_profile_to_post_comment extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        String commentTable = findTable(connection, "post_comment");
        String profileTable = findTable(connection, "band_member_profile");
        if (commentTable == null || profileTable == null) {
            return;
        }

        // ddl-auto(update)가 이미 컬럼을 만든 환경(로컬 등)에서는 건너뛴다 (멱등)
        if (findColumn(connection, commentTable, "band_member_profile_id") != null) {
            return;
        }

        boolean mysql = connection.getMetaData().getDatabaseProductName()
                .toLowerCase()
                .contains("mysql");
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE " + quote(commentTable, mysql)
                            + " ADD COLUMN " + quote("band_member_profile_id", mysql) + " BIGINT NULL"
            );
            statement.execute(
                    "ALTER TABLE " + quote(commentTable, mysql)
                            + " ADD CONSTRAINT fk_post_comment_band_member_profile"
                            + " FOREIGN KEY (" + quote("band_member_profile_id", mysql) + ")"
                            + " REFERENCES " + quote(profileTable, mysql) + " (id)"
            );
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

    private String findColumn(
            Connection connection,
            String tableName,
            String expectedName
    ) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(
                connection.getCatalog(), connection.getSchema(), tableName, "%")) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                if (expectedName.equalsIgnoreCase(columnName)) {
                    return columnName;
                }
            }
        }
        return null;
    }

    private String quote(String identifier, boolean mysql) {
        return mysql ? "`" + identifier + "`" : "\"" + identifier + "\"";
    }
}
