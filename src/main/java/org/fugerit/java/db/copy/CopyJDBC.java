package org.fugerit.java.db.copy;

import lombok.extern.slf4j.Slf4j;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * Utility class to copy database table data from a source database query 
 * to a destination database table dynamically using JDBC metadata.
 */
@Slf4j
public class CopyJDBC {

    private CopyJDBC() {
        // utility class private constructor
    }

    /**
     * Copy data from source query to destination table using default configuration.
     *
     * @param srcConn   Source database connection
     * @param destConn  Destination database connection
     * @param srcQuery  SQL Query to extract data from source
     * @param destTable Destination table name
     * @return Number of rows copied
     * @throws SQLException If any database error occurs
     */
    public static int copy(Connection srcConn, Connection destConn, String srcQuery, String destTable) throws SQLException {
        return copy(srcConn, destConn, srcQuery, destTable, new CopyConfig());
    }

    /**
     * Copy data from source query to destination table using custom configuration.
     *
     * @param srcConn   Source database connection
     * @param destConn  Destination database connection
     * @param srcQuery  SQL Query to extract data from source
     * @param destTable Destination table name
     * @param config    Copy configuration options
     * @return Number of rows copied
     * @throws SQLException If any database error occurs
     */
    public static int copy(Connection srcConn, Connection destConn, String srcQuery, String destTable, CopyConfig config) throws SQLException {
        if (config == null) {
            config = new CopyConfig();
        }

        log.info("Starting database copy operation: source query -> destination table '{}'", destTable);

        // 1. Truncate/delete destination table if configured
        if (config.isTruncateDest()) {
            String deleteSql = "DELETE FROM " + destTable;
            log.info("Truncating destination table using SQL: '{}'", deleteSql);
            try (Statement destStmt = destConn.createStatement()) {
                int deletedRows = destStmt.executeUpdate(deleteSql);
                log.info("Deleted {} rows from destination table '{}' before copying.", deletedRows, destTable);
            }
        }

        int totalCopied = 0;

        // 2. Query source database
        try (PreparedStatement srcPs = srcConn.prepareStatement(srcQuery);
             ResultSet srcRs = srcPs.executeQuery()) {

            ResultSetMetaData metaData = srcRs.getMetaData();
            int columnCount = metaData.getColumnCount();

            if (columnCount == 0) {
                log.warn("Source query returned 0 columns. Nothing to copy.");
                return 0;
            }

            // 3. Construct dynamic INSERT statement
            StringBuilder insertSql = new StringBuilder("INSERT INTO ").append(destTable).append(" (");
            StringBuilder valuesSql = new StringBuilder(" VALUES (");

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                insertSql.append(columnName);
                valuesSql.append("?");
                if (i < columnCount) {
                    insertSql.append(", ");
                    valuesSql.append(", ");
                }
            }
            insertSql.append(")").append(valuesSql).append(")");

            String finalInsertSql = insertSql.toString();
            log.debug("Constructed dynamic INSERT statement: {}", finalInsertSql);

            // 4. Prepare statement on destination and iterate over source records
            try (PreparedStatement destPs = destConn.prepareStatement(finalInsertSql)) {
                int batchCount = 0;
                int batchSize = config.getBatchSize();

                while (srcRs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        int columnType = metaData.getColumnType(i);
                        setStatementValue(srcRs, destPs, i, columnType);
                    }

                    destPs.addBatch();
                    batchCount++;
                    totalCopied++;

                    if (batchCount >= batchSize) {
                        log.debug("Executing batch of size {}", batchCount);
                        destPs.executeBatch();
                        batchCount = 0;
                    }
                }

                // Execute any remaining records in the last batch
                if (batchCount > 0) {
                    log.debug("Executing final batch of size {}", batchCount);
                    destPs.executeBatch();
                }
            }
        }

        log.info("Database copy completed successfully. Total rows copied: {}", totalCopied);
        return totalCopied;
    }

    /**
     * Map value from source ResultSet to destination PreparedStatement dynamically based on type.
     */
    private static void setStatementValue(ResultSet srcRs, PreparedStatement destPs, int columnIndex, int columnType) throws SQLException {
        if (columnType == Types.CLOB || columnType == Types.NCLOB) {
            Clob clob = srcRs.getClob(columnIndex);
            if (clob != null) {
                destPs.setCharacterStream(columnIndex, clob.getCharacterStream());
            } else {
                destPs.setNull(columnIndex, columnType);
            }
        } else if (columnType == Types.BLOB) {
            Blob blob = srcRs.getBlob(columnIndex);
            if (blob != null) {
                destPs.setBinaryStream(columnIndex, blob.getBinaryStream());
            } else {
                destPs.setNull(columnIndex, columnType);
            }
        } else {
            Object obj = srcRs.getObject(columnIndex);
            if (obj != null) {
                destPs.setObject(columnIndex, obj);
            } else {
                destPs.setNull(columnIndex, columnType);
            }
        }
    }
}
