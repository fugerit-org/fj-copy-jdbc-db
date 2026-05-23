package org.fugerit.java.db.copy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;

public class CopyJDBCTest {

    private Connection srcConn;
    private Connection destConn;

    @BeforeEach
    public void setup() throws SQLException {
        srcConn = DriverManager.getConnection("jdbc:h2:mem:src_db;DB_CLOSE_DELAY=-1");
        destConn = DriverManager.getConnection("jdbc:h2:mem:dest_db;DB_CLOSE_DELAY=-1");

        try (Statement s = srcConn.createStatement()) {
            s.execute("CREATE TABLE SRC_TABLE (id INT PRIMARY KEY, name VARCHAR(255), flag BOOLEAN, created_at TIMESTAMP, details CLOB, data BLOB)");
        }

        try (Statement s = destConn.createStatement()) {
            s.execute("CREATE TABLE DEST_TABLE (id INT PRIMARY KEY, name VARCHAR(255), flag BOOLEAN, created_at TIMESTAMP, details CLOB, data BLOB)");
        }
    }

    @AfterEach
    public void tearDown() throws SQLException {
        try (Statement s = srcConn.createStatement()) {
            s.execute("DROP TABLE SRC_TABLE");
        }
        try (Statement s = destConn.createStatement()) {
            s.execute("DROP TABLE DEST_TABLE");
        }
        srcConn.close();
        destConn.close();
    }

    @Test
    public void testCopyBasic() throws Exception {
        // Insert sample rows
        try (PreparedStatement ps = srcConn.prepareStatement("INSERT INTO SRC_TABLE (id, name, flag, created_at, details, data) VALUES (?, ?, ?, ?, ?, ?)")) {
            // Row 1: normal
            ps.setInt(1, 1);
            ps.setString(2, "Alice");
            ps.setBoolean(3, true);
            ps.setTimestamp(4, Timestamp.valueOf("2026-05-23 12:00:00"));
            ps.setCharacterStream(5, new java.io.StringReader("Some clob data"));
            ps.setBinaryStream(6, new ByteArrayInputStream("Some blob bytes".getBytes(StandardCharsets.UTF_8)));
            ps.executeUpdate();

            // Row 2: with nulls
            ps.setInt(1, 2);
            ps.setString(2, "Bob");
            ps.setNull(3, java.sql.Types.BOOLEAN);
            ps.setNull(4, java.sql.Types.TIMESTAMP);
            ps.setNull(5, java.sql.Types.CLOB);
            ps.setNull(6, java.sql.Types.BLOB);
            ps.executeUpdate();
        }

        int copied = CopyJDBC.copy(srcConn, destConn, "SELECT * FROM SRC_TABLE ORDER BY id", "DEST_TABLE");
        assertEquals(2, copied);

        // Verify destination rows
        try (Statement s = destConn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM DEST_TABLE ORDER BY id")) {
            
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("id"));
            assertEquals("Alice", rs.getString("name"));
            assertTrue(rs.getBoolean("flag"));
            assertEquals(Timestamp.valueOf("2026-05-23 12:00:00"), rs.getTimestamp("created_at"));
            assertEquals("Some clob data", rs.getString("details"));
            assertArrayEquals("Some blob bytes".getBytes(StandardCharsets.UTF_8), rs.getBytes("data"));

            assertTrue(rs.next());
            assertEquals(2, rs.getInt("id"));
            assertEquals("Bob", rs.getString("name"));
            assertFalse(rs.getBoolean("flag")); // getBoolean returns false for SQL NULL
            assertTrue(rs.wasNull());
            assertNull(rs.getTimestamp("created_at"));
            assertNull(rs.getString("details"));
            assertNull(rs.getBytes("data"));

            assertFalse(rs.next());
        }
    }

    @Test
    public void testCopyTruncate() throws Exception {
        // Pre-populate destination table
        try (Statement s = destConn.createStatement()) {
            s.execute("INSERT INTO DEST_TABLE (id, name) VALUES (99, 'Old Record')");
        }

        // Insert into source table
        try (Statement s = srcConn.createStatement()) {
            s.execute("INSERT INTO SRC_TABLE (id, name) VALUES (1, 'New Record')");
        }

        CopyConfig config = CopyConfig.builder().truncateDest(true).build();
        int copied = CopyJDBC.copy(srcConn, destConn, "SELECT * FROM SRC_TABLE", "DEST_TABLE", config);
        assertEquals(1, copied);

        // Verify that Old Record was deleted and only New Record exists
        try (Statement s = destConn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM DEST_TABLE")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("id"));
            assertEquals("New Record", rs.getString("name"));
            assertFalse(rs.next());
        }
    }

    @Test
    public void testCopyBatchSize() throws Exception {
        // Insert multiple rows into source
        try (PreparedStatement ps = srcConn.prepareStatement("INSERT INTO SRC_TABLE (id, name) VALUES (?, ?)")) {
            for (int i = 1; i <= 5; i++) {
                ps.setInt(1, i);
                ps.setString(2, "Name_" + i);
                ps.executeUpdate();
            }
        }

        // Copy with batch size 2 (triggering multiple batches)
        CopyConfig config = CopyConfig.builder().batchSize(2).build();
        int copied = CopyJDBC.copy(srcConn, destConn, "SELECT id, name FROM SRC_TABLE", "DEST_TABLE", config);
        assertEquals(5, copied);

        // Verify count
        try (Statement s = destConn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM DEST_TABLE")) {
            assertTrue(rs.next());
            assertEquals(5, rs.getInt(1));
        }
    }

    @Test
    public void testEmptySource() throws Exception {
        int copied = CopyJDBC.copy(srcConn, destConn, "SELECT * FROM SRC_TABLE", "DEST_TABLE");
        assertEquals(0, copied);

        try (Statement s = destConn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM DEST_TABLE")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }
}
