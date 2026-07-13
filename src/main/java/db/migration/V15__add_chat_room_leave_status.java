package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.*;

public class V15__add_chat_room_leave_status extends BaseJavaMigration {
    @Override public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String table = findTable(connection, "chat_rooms");
        if (table == null) return;
        boolean mysql = connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql");
        if (!hasColumn(connection, table, "sender_left_at")) addColumn(connection, table, "sender_left_at", mysql);
        if (!hasColumn(connection, table, "recipient_left_at")) addColumn(connection, table, "recipient_left_at", mysql);
    }
    private void addColumn(Connection c, String table, String column, boolean mysql) throws Exception {
        try (Statement s = c.createStatement()) {
            s.execute("ALTER TABLE " + q(table, mysql) + " ADD COLUMN " + q(column, mysql) + " TIMESTAMP NULL");
        }
    }
    private String findTable(Connection c, String expected) throws Exception {
        try (ResultSet r = c.getMetaData().getTables(c.getCatalog(), c.getSchema(), "%", new String[]{"TABLE"})) {
            while (r.next()) if (expected.equalsIgnoreCase(r.getString("TABLE_NAME"))) return r.getString("TABLE_NAME");
        } return null;
    }
    private boolean hasColumn(Connection c, String table, String expected) throws Exception {
        try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), c.getSchema(), table, "%")) {
            while (r.next()) if (expected.equalsIgnoreCase(r.getString("COLUMN_NAME"))) return true;
        } return false;
    }
    private String q(String value, boolean mysql) { return mysql ? "`" + value + "`" : "\"" + value + "\""; }
}
