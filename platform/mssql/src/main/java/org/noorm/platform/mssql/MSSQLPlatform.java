package org.noorm.platform.mssql;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import org.noorm.platform.IMetadata;
import org.noorm.platform.IPlatform;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@ext.secunet.com
 *         Date: 11.02.14
 *         Time: 13:52
 */
public class MSSQLPlatform implements IPlatform {

    private static final String SQL_SERVER_PLATFORM = "SQLServer";

    private final MSSQLMetadata msSQLMetadata = MSSQLMetadata.getInstance();

    /**
     * Returns the name of platform service provider
     *
     * @return the platform name
     */
    @Override
    public String getName() {

        return SQL_SERVER_PLATFORM;
    }

    /**
     * Creates a platform specific data source.
     *
     * @param pURL the connection URL
     * @param pUsername the username
     * @param pPassword the password
     * @return the established data source
     */
    @Override
    public DataSource getDataSource(String pURL, String pUsername, String pPassword) throws SQLException {

        final SQLServerDataSource dataSource = new SQLServerDataSource();
        dataSource.setUser(pUsername);
        dataSource.setPassword(pPassword);
        dataSource.setURL(pURL);
        return dataSource;
    }

    /**
     * Validates the platform specific data source
     *
     * @param pDataSource the data source
     * @return a textual summary of the data source validation
     */
    @Override
    public String validateDataSource(DataSource pDataSource) throws SQLException {

        final StringBuilder validationInfo = new StringBuilder();
        validationInfo.append("Validating data source. ");
        if (pDataSource instanceof SQLServerDataSource) {
            validationInfo.append("Connection parameters: ");
            validationInfo.append(";URL: ");
            validationInfo.append(((SQLServerDataSource) pDataSource).getURL());
            validationInfo.append(";Username: ");
            validationInfo.append(((SQLServerDataSource) pDataSource).getUser());
        } else {
            validationInfo.append("Unable to retrieve connection parameters from data source. [");
            validationInfo.append(pDataSource.getClass().getName());
            validationInfo.append("]");
        }
        return validationInfo.toString();
    }

    /**
     * Returns the platform specific query to retrieve a sequence value generated by the database.
     *
     * @param pSequenceName the name of the database sequence
     * @return the SELECT statement to retrieve a single sequence value for the given platform
     */
    @Override
    public String getSequenceQuery(final String pSequenceName) {

        final String sequenceQuery = "SELECT NEXT VALUE FOR  ".concat(pSequenceName);
        return sequenceQuery;
    }

    /**
     * Executes a batch over a prepared statement.
     * Different databases and JDBC drivers handle the update count differently. To get a reliable update,
     * a platform specific implementation is required.
     *
     * @param pPreparedStatement the prepared statement ready for executing the next batch
     * @return the reliable update count for the platform in use
     */
    @Override
    public int executeBatchWithReliableCount(final PreparedStatement pPreparedStatement) throws SQLException {

        final int[] batchCounts = pPreparedStatement.executeBatch();
        int updateCount = 0;
        for (int i = 0; i < batchCounts.length; i++) {
            updateCount += batchCounts[i];
        }
        return updateCount;
    }

    /**
     * Sets an object value for an DML statement (INSERT, UPDATE, DELETE).
     *
     * @param pStmt           the prepared SQL statement
     * @param pValue          the parameter value to be set
     * @param pParameterIndex the index of the parameter
     * @param pSQLType        the SQL type. Usually one type specified in java.sql.Types or a proprietary type
     * @throws SQLException JDBC driver exception
     */
    @Override
    public void setObject(final PreparedStatement pStmt,
                          final Object pValue,
                          final int pParameterIndex,
                          final int pSQLType) throws SQLException {

        if (pValue == null) {
            pStmt.setNull(pParameterIndex, pSQLType);
        } else {
            pStmt.setObject(pParameterIndex, pValue, pSQLType);
        }
    }

    /**
     * Provides database metadata for code generation and validation of generated code.
     *
     * @return the platform specific implementation of the metadata retrieval functionality
     */
    @Override
    public IMetadata getMetadata() {

        return msSQLMetadata;
    }
}