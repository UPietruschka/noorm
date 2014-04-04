package org.noorm.platform;

import org.noorm.jdbc.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@ext.secunet.com
 *         Date: 11.02.14
 *         Time: 14:06
 *
 * Instantiates the database platform specific implementation.
 * When the data source is passed as an argument, the factory first tries to derive the platform from the data
 * source, i.e., the potentially valid platform name is ignored in this case. Giving the data source analysis
 * precedende over the concrete platform name has the advantage that the same configuration can work for multiple
 * database platforms. The latter does not work, when the data source does not provide the required information
 * about the platform, e.g., when a generic pooled data source is in use (e.g. DBCP).
 */
public class PlatformFactory {

    private static final Logger log = LoggerFactory.getLogger(PlatformFactory.class);

    public static IPlatform createPlatform(final Class pDataSourceClass, final String pPlatform) {

        final Iterator<IPlatform> platforms = ServiceLoader.load(IPlatform.class).iterator();
        while (platforms.hasNext()) {
            final IPlatform platform = platforms.next();
            final String platformName = platform.getName();
            log.info("NoORM platform service provider found for : ".concat(platformName));

            if (pDataSourceClass.getName().contains(platformName)) {
                return platform;
            }
            if (pPlatform.equals(platformName)) {
                return platform;
            }
        }
        throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_PLATFORM, pDataSourceClass.getName());
    }

    public static IPlatform createPlatform(final String pDatabaseProductName) {

        final Iterator<IPlatform> platforms = ServiceLoader.load(IPlatform.class).iterator();
        while (platforms.hasNext()) {
            final IPlatform platform = platforms.next();
            final String platformName = platform.getName();
            log.info("NoORM platform service provider found for : ".concat(platformName));

            if (pDatabaseProductName.equals(platformName)) {
                return platform;
            }
        }
        throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_PLATFORM, pDatabaseProductName);
    }
}
