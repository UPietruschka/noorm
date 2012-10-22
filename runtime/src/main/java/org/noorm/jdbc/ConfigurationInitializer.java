package org.noorm.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This class reads the NoORM configuration file (noorm.xml or noorm.properties), if it exists and initializes the
 * configured data source(s), if appropriate.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public class ConfigurationInitializer {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationInitializer.class);

    public static final String NOORM_PROPERTIES_FILENAME = "/META-INF/noorm.properties";
    public static final String NOORM_XML_FILENAME = "/META-INF/noorm.xml";
    public static final String DATABASE_JNDINAME = "database.jndiname";
    public static final String DATABASE_PASSWORD = "database.password";
    public static final String DATABASE_URL = "database.url";
    public static final String DATABASE_USERNAME = "database.username";
    public static final String DATABASE_BATCH_UPDATE_SIZE = "database.batch_update_size";
    public static final String DEBUG_MODE = "debug.mode";
    public static final String DEBUG_JDWP_HOST = "debug.host";
    public static final String DEBUG_JDWP_PORT = "debug.port";

    private static final String DEFAULT_DATA_SOURCE_NAME = "_DEFAULT_";

    private final Map noormProperties = new HashMap();

    public DataSourceConfiguration init() {

        final Properties noormFileProperties = loadNoormProperties();
        return initNoormProperties(noormFileProperties);
    }

    private DataSourceConfiguration initNoormProperties(final Properties pNoORMFileProperties) {

        final DataSourceConfiguration dataSourceConfiguration = new DataSourceConfiguration();

        final String batchUpdateSizeProp = pNoORMFileProperties.getProperty(DATABASE_BATCH_UPDATE_SIZE);
        if (batchUpdateSizeProp != null && !batchUpdateSizeProp.isEmpty()) {
            try {
                dataSourceConfiguration.setDatabaseBatchUpdateSize(Integer.parseInt(batchUpdateSizeProp));
                log.info("Setting ".concat(DATABASE_BATCH_UPDATE_SIZE).concat(" = ").concat(batchUpdateSizeProp));
            } catch (NumberFormatException ex) {
                throw new DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE, ex);
            }
        } else {
            final String batchUpdateSizeS =
                    (new Integer(dataSourceConfiguration.getDatabaseBatchUpdateSize()).toString());
            log.info("Setting (default) ".concat(DATABASE_BATCH_UPDATE_SIZE).concat(" = ").concat(batchUpdateSizeS));
        }

        final String debugModeProp = pNoORMFileProperties.getProperty(DEBUG_MODE);
        if (debugModeProp != null && debugModeProp.toLowerCase().equals("true")) {
            dataSourceConfiguration.setDebugMode(true);
            log.info("Setting ".concat(DEBUG_MODE).concat(" = true"));
        } else {
            log.info("Setting (default) ".concat(DEBUG_MODE).concat(" = false"));
        }

        final String debugHostProp = pNoORMFileProperties.getProperty(DEBUG_JDWP_HOST);
        if (debugHostProp != null && !debugHostProp.isEmpty()) {
            dataSourceConfiguration.setDebugJDWPHost(debugHostProp);
            log.info("Setting ".concat(DEBUG_JDWP_HOST).concat(" = ").concat(debugHostProp));
        } else {
            log.info("Setting (default) ".concat(DEBUG_JDWP_HOST).concat(" = ")
                    .concat(dataSourceConfiguration.getDebugJDWPHost()));
        }

        final String debugPortProp = pNoORMFileProperties.getProperty(DEBUG_JDWP_PORT);
        if (debugPortProp != null && !debugPortProp.isEmpty()) {
            dataSourceConfiguration.setDebugJDWPPort(debugPortProp);
            log.info("Setting ".concat(DEBUG_JDWP_PORT).concat(" = ").concat(debugPortProp));
        } else {
            log.info("Setting (default) ".concat(DEBUG_JDWP_PORT).concat(" = ")
                    .concat(dataSourceConfiguration.getDebugJDWPPort()));
        }

        final String databaseJNDIName = pNoORMFileProperties.getProperty(DATABASE_JNDINAME);
        if (databaseJNDIName != null && !databaseJNDIName.isEmpty()) {
            dataSourceConfiguration.setDatabaseJNDIName(databaseJNDIName);
            log.info("Setting ".concat(DATABASE_JNDINAME).concat(" = ").concat(databaseJNDIName));
        }

        final String databaseURL = pNoORMFileProperties.getProperty(DATABASE_URL);
        if (databaseURL != null && !databaseURL.isEmpty()) {
            dataSourceConfiguration.setDatabaseURL(databaseURL);
            log.info("Setting ".concat(DATABASE_URL).concat(" = ").concat(databaseURL));
        }

        final String databaseUsername = pNoORMFileProperties.getProperty(DATABASE_USERNAME);
        if (databaseUsername != null && !databaseUsername.isEmpty()) {
            dataSourceConfiguration.setDatabaseUsername(databaseUsername);
            log.info("Setting ".concat(DATABASE_USERNAME).concat(" = ").concat(databaseUsername));
        }

        final String databasePassword = pNoORMFileProperties.getProperty(DATABASE_PASSWORD);
        if (databasePassword != null && !databasePassword.isEmpty()) {
            dataSourceConfiguration.setDatabasePassword(databasePassword);
            log.info("Setting ".concat(DATABASE_PASSWORD).concat(" = ").concat(databasePassword));
        }

        return dataSourceConfiguration;
    }

    private Properties loadNoormProperties() {

        log.info("Trying to load configuration file ".concat(NOORM_XML_FILENAME));
        final Properties noormFileProperties = new Properties();
        InputStream is = this.getClass().getResourceAsStream(NOORM_XML_FILENAME);
        try {
            if (is != null) {
                noormFileProperties.loadFromXML(is);
                log.info("Configuration file ".concat(NOORM_XML_FILENAME).concat(" loaded."));
            } else {
                log.info("Configuration file ".concat(NOORM_XML_FILENAME).concat(" not found."));
                log.info("Trying to load configuration file ".concat(NOORM_PROPERTIES_FILENAME));
                is = this.getClass().getResourceAsStream(NOORM_PROPERTIES_FILENAME);
                if (is != null) {
                    noormFileProperties.load(is);
                    log.info("Configuration file ".concat(NOORM_PROPERTIES_FILENAME).concat(" loaded."));
                } else {
                    log.info("Configuration file ".concat(NOORM_PROPERTIES_FILENAME).concat(" not found."));
                    throw new FileNotFoundException();
                }
            }
        } catch (final FileNotFoundException ex) {
            // File noorm.properties is optional, failing to load the properties is considered to be
            // an exception only when the reason for the failure is not a missing file.
        } catch (final IOException ex) {
            throw new DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE,
                    "Loading of noorm.properties failed.");
        }
        return noormFileProperties;
    }
}
