package org.fugerit.java.db.copy;

import org.fugerit.java.core.db.connect.ConnectionFactoryCloseable;
import org.fugerit.java.core.db.connect.ConnectionFactoryImpl;
import org.fugerit.java.core.function.SafeFunction;
import org.fugerit.java.core.lang.helpers.ClassHelper;

import java.sql.Connection;
import java.util.Properties;

public class CopyJDBCTool {

    private CopyJDBCTool() {}

    public static final String PARAM_SRC_QUERY = "src-query";

    public static final String PARAM_DEST_TABLE = "dest-table";

    public static long copyFromParams(Properties params) {
        return SafeFunction.get( () -> {
            try (ConnectionFactoryCloseable srcCf = ConnectionFactoryImpl.wrap(
                    ConnectionFactoryImpl.newInstance( params, "src", ClassHelper.getDefaultClassLoader() ) ) ;
                 ConnectionFactoryCloseable destCf = ConnectionFactoryImpl.wrap(
                         ConnectionFactoryImpl.newInstance( params, "dest", ClassHelper.getDefaultClassLoader() ) );
                 Connection srcConn = srcCf.getConnection();
                 Connection destConn = destCf.getConnection()
            ) {
                String srcQuery = params.getProperty( PARAM_SRC_QUERY );
                String destTable = params.getProperty( PARAM_DEST_TABLE );
                return CopyJDBC.copy( srcConn, destConn, srcQuery, destTable );
            }
        } );
    }

}
