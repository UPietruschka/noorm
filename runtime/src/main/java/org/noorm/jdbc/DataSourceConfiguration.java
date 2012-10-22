package org.noorm.jdbc;

/**
 * This class represents the configuration parameters available for NoORM data sources through configuration files
 * noorm.xml or noorm.properties.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public class DataSourceConfiguration {

    private String databaseJNDIName;
    private String databaseUsername;
    private String databasePassword;
    private String databaseURL;
    private int databaseBatchUpdateSize = 100;
    private boolean debugMode = false;
    private String debugJDWPPort = "4000";
    private String debugJDWPHost = "localhost";

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
