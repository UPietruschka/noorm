package org.noorm.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
    public static final String DATASOURCE_NAMES = "datasource.names";
    public static final String DATABASE_JNDINAME = "database.jndiname";
    public static final String DATABASE_PASSWORD = "database.password";
    public static final String DATABASE_URL = "database.url";
    public static final String DATABASE_USERNAME = "database.username";
    public static final String DATABASE_BATCH_UPDATE_SIZE = "database.batch_update_size";
    public static final String DEBUG_MODE = "debug.mode";
    public static final String DEBUG_JDWP_HOST = "debug.host";
    public static final String DEBUG_JDWP_PORT = "debug.port";

    public static final String DEFAULT_DATA_SOURCE_NAME = "";

    /**
     * Reads the NoORM configuration file and maps the configuration values to a map with data source
     * configurations. Configuration parameter "datasource.names" indicates, whether more than a single
     * data source configuration is available (for single data sources, this parameter is not required).
     * When multiple data sources are configured, the data source names must be used as prefix for all
     * other configuration parameters.
     *
     * @return configured data source configurations
     */
    public Map<String, DataSourceConfiguration> init() {

        final Map<String, DataSourceConfiguration> dataSources = new HashMap<String, DataSourceConfiguration>();
        final Properties noormFileProperties = loadNoormProperties();
        final String dataSourceNameList = noormFileProperties.getProperty(DATASOURCE_NAMES);
        if (dataSourceNameList != null && !dataSourceNameList.isEmpty()) {
            // Multiple data source names have been configured.
            final String[] dataSourceNames = dataSourceNameList.split(",");
            for (final String dataSourceName : dataSourceNames) {
                final DataSourceConfiguration dataSourceConfiguration =
                        initNoormProperties(noormFileProperties, dataSourceName);
                dataSources.put(dataSourceName, dataSourceConfiguration);
            }
        } else {
            final DataSourceConfiguration dataSourceConfiguration =
                    initNoormProperties(noormFileProperties, DEFAULT_DATA_SOURCE_NAME);
            dataSources.put(DEFAULT_DATA_SOURCE_NAME, dataSourceConfiguration);
        }
        return dataSources;
    }

    private DataSourceConfiguration initNoormProperties(final Properties pNoORMFileProperties,
                                                        final String pDataSourceName) {

        final DataSourceConfiguration dataSourceConfiguration = new DataSourceConfiguration();

        String key = pDataSourceName.concat(DATABASE_BATCH_UPDATE_SIZE);
        final String batchUpdateSizeProp = pNoORMFileProperties.getProperty(key);
        if (batchUpdateSizeProp != null && !batchUpdateSizeProp.isEmpty()) {
            try {
                dataSourceConfiguration.setDatabaseBatchUpdateSize(Integer.parseInt(batchUpdateSizeProp));
                log.info("Setting ".concat(key).concat(" = ").concat(batchUpdateSizeProp));
            } catch (NumberFormatException ex) {
                throw new DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE, ex);
            }
        } else {
            final String batchUpdateSizeS =
                    (new Integer(dataSourceConfiguration.getDatabaseBatchUpdateSize()).toString());
            log.info("Setting (default) ".concat(key).concat(" = ").concat(batchUpdateSizeS));
        }

        key = pDataSourceName.concat(DEBUG_MODE);
        final String debugModeProp = pNoORMFileProperties.getProperty(key);
        if (debugModeProp != null && debugModeProp.toLowerCase().equals("true")) {
            dataSourceConfiguration.setDebugMode(true);
            log.info("Setting ".concat(key).concat(" = true"));
        } else {
            log.info("Setting (default) ".concat(key).concat(" = false"));
        }

        key = pDataSourceName.concat(DEBUG_JDWP_HOST);
        final String debugHostProp = pNoORMFileProperties.getProperty(key);
        if (debugHostProp != null && !debugHostProp.isEmpty()) {
            dataSourceConfiguration.setDebugJDWPHost(debugHostProp);
            log.info("Setting ".concat(key).concat(" = ").concat(debugHostProp));
        } else {
            log.info("Setting (default) ".concat(key).concat(" = ")
                    .concat(dataSourceConfiguration.getDebugJDWPHost()));
        }

        key = pDataSourceName.concat(DEBUG_JDWP_PORT);
        final String debugPortProp = pNoORMFileProperties.getProperty(key);
        if (debugPortProp != null && !debugPortProp.isEmpty()) {
            dataSourceConfiguration.setDebugJDWPPort(debugPortProp);
            log.info("Setting ".concat(key).concat(" = ").concat(debugPortProp));
        } else {
            log.info("Setting (default) ".concat(key).concat(" = ")
                    .concat(dataSourceConfiguration.getDebugJDWPPort()));
        }

        key = pDataSourceName.concat(DATABASE_JNDINAME);
        final String databaseJNDIName = pNoORMFileProperties.getProperty(key);
        if (databaseJNDIName != null && !databaseJNDIName.isEmpty()) {
            dataSourceConfiguration.setDatabaseJNDIName(databaseJNDIName);
            log.info("Setting ".concat(key).concat(" = ").concat(databaseJNDIName));
        }

        key = pDataSourceName.concat(DATABASE_URL);
        final String databaseURL = pNoORMFileProperties.getProperty(key);
        if (databaseURL != null && !databaseURL.isEmpty()) {
            dataSourceConfiguration.setDatabaseURL(databaseURL);
            log.info("Setting ".concat(key).concat(" = ").concat(databaseURL));
        }

        key = pDataSourceName.concat(DATABASE_USERNAME);
        final String databaseUsername = pNoORMFileProperties.getProperty(key);
        if (databaseUsername != null && !databaseUsername.isEmpty()) {
            dataSourceConfiguration.setDatabaseUsername(databaseUsername);
            log.info("Setting ".concat(key).concat(" = ").concat(databaseUsername));
        }

        key = pDataSourceName.concat(DATABASE_PASSWORD);
        final String databasePassword = pNoORMFileProperties.getProperty(key);
        if (databasePassword != null && !databasePassword.isEmpty()) {
            dataSourceConfiguration.setDatabasePassword(databasePassword);
            log.info("Setting ".concat(key).concat(" = ").concat(databasePassword));
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
