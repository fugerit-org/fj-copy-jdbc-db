package org.fugerit.java.db.copy;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Configuration options for the JDBC database copy operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyConfig {

    public static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * The number of rows to insert in each batch.
     */
    @Builder.Default
    private int batchSize = DEFAULT_BATCH_SIZE;

    /**
     * Whether to delete/truncate all rows in the destination table before copying.
     */
    @Builder.Default
    private boolean truncateDest = false;
}
