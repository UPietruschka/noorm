package org.noorm.platform;

import org.noorm.jdbc.DataAccessException;
import org.noorm.platform.mssql.MSSQLPlatform;
import org.noorm.platform.oracle.OraclePlatform;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@ext.secunet.com
 *         Date: 11.02.14
 *         Time: 14:06
 *         <p/>
 *         Project class implementation
 */
public class PlatformFactory {

    public static final String ORACLE_PLATFORM = "Oracle";
    public static final String SQL_SERVER_PLATFORM = "SQLServer";

    public static IPlatform createPlatform(final Class pDataSourceClass) {

        if (pDataSourceClass.getName().contains(ORACLE_PLATFORM)) {
            return new OraclePlatform();
        }
        if (pDataSourceClass.getName().contains(SQL_SERVER_PLATFORM)) {
            return new MSSQLPlatform();
        }
        throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_PLATFORM, pDataSourceClass.getName());
    }

    public static IPlatform createPlatform(final String pDatabaseProductName) {

        if (pDatabaseProductName.equals(ORACLE_PLATFORM)) {
            return new OraclePlatform();
        }
        if (pDatabaseProductName.equals(SQL_SERVER_PLATFORM)) {
            return new MSSQLPlatform();
        }
        throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_PLATFORM, pDatabaseProductName);
    }
}
