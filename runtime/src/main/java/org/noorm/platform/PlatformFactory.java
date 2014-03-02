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

    public static IPlatform createPlatform(final Class pDataSourceClass) {

        if (pDataSourceClass.getName().contains("Oracle")) {
            return new OraclePlatform();
        }
        if (pDataSourceClass.getName().contains("SQLServer")) {
            return new MSSQLPlatform();
        }
        throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_PLATFORM, pDataSourceClass.getName());
    }

    public static IPlatform createPlatform(final String pDatabaseProductName) {

        if (pDatabaseProductName.equals("Oracle")) {
            return new OraclePlatform();
        }
        if (pDatabaseProductName.equals("Microsoft SQL Server")) {
            return new MSSQLPlatform();
        }
        throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_PLATFORM, pDatabaseProductName);
    }
}
