package org.noorm.platform;

import org.noorm.jdbc.DataAccessException;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@ext.secunet.com
 *         Date: 11.02.14
 *         Time: 14:06
 *         <p/>
 *         Project class implementation
 */
public class PlatformFactory {

    private static final String ORACLE_PRODUCT_NAME = "Oracle";
    private static final String MSSQL_PRODUCT_NAME = "Microsoft SQL Server";

    public static IPlatform createPlatform(final String pDatabaseProductName) {

        if (ORACLE_PRODUCT_NAME.equals(pDatabaseProductName)) {
            return new OraclePlatform();
        }
        if (MSSQL_PRODUCT_NAME.equals(pDatabaseProductName)) {
            return new MSSQLPlatform();
        }
        throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_PLATFORM, pDatabaseProductName);
    }
}
