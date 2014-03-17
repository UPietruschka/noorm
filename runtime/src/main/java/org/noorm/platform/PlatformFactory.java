package org.noorm.platform;

import org.noorm.jdbc.DataAccessException;
import org.noorm.platform.mssql.MSSQLPlatform;
import org.noorm.platform.oracle.OraclePlatform;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@ext.secunet.com
 *         Date: 11.02.14
 *         Time: 14:06
 *         <p/>
 * Instantiates the database platform specific implementation.
 * When the data source is passed as an argument, the factory first tries to derive the platform from the data
 * source, i.e., the potentially valid platform name is ignored in this case. Giving the data source analysis
 * precedende over the concrete platform name has the advantage that the same configuration can work for multiple
 * database platforms. The latter does not work, when the data source does not provide the required information
 * about the platform, e.g., when a generic pooled data source is in use (e.g. DBCP).
 */
public class PlatformFactory {

    public static final String ORACLE_PLATFORM = "Oracle";
    public static final String SQL_SERVER_PLATFORM = "SQLServer";

    public static IPlatform createPlatform(final Class pDataSourceClass, final String pPlatform) {

        if (pDataSourceClass.getName().contains(ORACLE_PLATFORM)) {
            return new OraclePlatform();
        }
        if (pDataSourceClass.getName().contains(SQL_SERVER_PLATFORM)) {
            return new MSSQLPlatform();
        }
        if (pPlatform.equals(ORACLE_PLATFORM)) {
            return new OraclePlatform();
        }
        if (pPlatform.equals(SQL_SERVER_PLATFORM)) {
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
