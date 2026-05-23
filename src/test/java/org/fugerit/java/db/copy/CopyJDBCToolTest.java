package org.fugerit.java.db.copy;

import org.fugerit.java.core.util.PropsIO;
import org.fugerit.java.core.util.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class CopyJDBCToolTest {

    @Test
    void testTool() throws Exception {
        Properties params = PropsIO.loadFromClassLoaderSafe( "config/copy-jdbc-db.properties" );
        long result = CopyJDBCTool.copyFromParams( params );
        Assertions.assertEquals( 2L, result );
    }

}
