package org.noorm.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the configuration parameters available for NoORM data sources through configuration files
 * noorm.xml or noorm.properties.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public class DataSourceConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfiguration.class);

    private String databaseJNDIName;
    private String databaseUsername;
    private String databasePassword;
    private String databaseURL;
    private int databaseBatchUpdateSize = 100;
    private boolean debugMode = false;
    private String debugJDWPPort = "4000";
    private String debugJDWPHost = "localhost";

    /**
     * Validates the given data source configuration. Since most configuration parameters may contain a variety of
     * syntax options to specify its content, this method only checks, if either the JNDI name is provided or URL,
     * username and password altogether.
     * In case of a failed validation, DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE) is thrown.
     */
    public void validate() {

        if (databaseJNDIName == null || databaseJNDIName.isEmpty() ) {
            final String noJNDIConfig = "Either provide a JNDI data source configuration or provide all required" +
                    " parameters for explicit data source instantiation. ";
            if (databaseURL == null || databaseURL.isEmpty()) {
                final String errMsg = "JDBC data source URL not properly configured. ".concat(noJNDIConfig);
                log.error(errMsg);
                throw new DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE, errMsg);
            }
            if (databaseUsername == null || databaseUsername.isEmpty()) {
                final String errMsg = "JDBC data source username not properly configured. ".concat(noJNDIConfig);
                log.error(errMsg);
                throw new DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE, errMsg);
            }
            if (databasePassword == null || databasePassword.isEmpty()) {
                final String errMsg = "JDBC data source password not properly configured. ".concat(noJNDIConfig);
                log.error(errMsg);
                throw new DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE, errMsg);
            }
        }
    }

    public String getDatabaseJNDIName() {
        return databaseJNDIName;
    }

    public void setDatabaseJNDIName(final String pDatabaseJNDIName) {
        databaseJNDIName = pDatabaseJNDIName;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public void setDatabaseUsername(final String pDatabaseUsername) {
        databaseUsername = pDatabaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(final String pDatabasePassword) {
        databasePassword = pDatabasePassword;
    }

    public String getDatabaseURL() {
        return databaseURL;
    }

    public void setDatabaseURL(final String pDatabaseURL) {
        databaseURL = pDatabaseURL;
    }

    public int getDatabaseBatchUpdateSize() {
        return databaseBatchUpdateSize;
    }

    public void setDatabaseBatchUpdateSize(final int pDatabaseBatchUpdateSize) {
        databaseBatchUpdateSize = pDatabaseBatchUpdateSize;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(final boolean pDebugMode) {
        debugMode = pDebugMode;
    }

    public String getDebugJDWPPort() {
        return debugJDWPPort;
    }

    public void setDebugJDWPPort(final String pDebugJDWPPort) {
        debugJDWPPort = pDebugJDWPPort;
    }

    public String getDebugJDWPHost() {
        return debugJDWPHost;
    }

    public void setDebugJDWPHost(final String pDebugJDWPHost) {
        debugJDWPHost = pDebugJDWPHost;
    }
}
