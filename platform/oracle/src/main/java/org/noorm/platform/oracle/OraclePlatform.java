package org.noorm.platform.oracle;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.ucp.jdbc.PoolDataSource;
import org.noorm.jdbc.platform.IMetadata;
import org.noorm.jdbc.platform.IPlatform;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@ext.secunet.com
 *         Date: 11.02.14
 *         Time: 13:52
 */
public class OraclePlatform implements IPlatform {

    public static final String ORACLE_PLATFORM = "Oracle";
    public static final String NOORM_ID_LIST_DB_TYPE_NAME = "NUM_ARRAY";

    private final OracleMetadata oracleMetadata = OracleMetadata.getInstance();

    /**
     * Returns the name of platform service provider
     *
     * @return the platform name
     */
    @Override
    public String getName() {

        return ORACLE_PLATFORM;
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

		final OracleDataSource oracleDataSource = new OracleDataSource();
		oracleDataSource.setURL(pURL);
		oracleDataSource.setUser(pUsername);
		oracleDataSource.setPassword(pPassword);

        // We enable the Oracle connection cache integrated with the Oracle JDBC driver.
        // Even for single-threaded stand-alone applications using a connection pool/cache makes sense.
        // Like any other ORM tool, NoORM does not manage data sources, but simply uses the JDBC API.
        // When transactions are not handled explicitly by the calling application, the implicit
        // auto-commit mode will cause connections to be closed with every single database call. Though
        // DataSourceProvider could retain connections for some time, its primary function is not the
        // maintenance of a connection cache or pool, so this job is delegated to the used data source,
        // which should provide some caching functionality for any usage scenario.
        // Unfortunately, Oracle stopped development of the build-in connection cache, so, starting with
        // Oracle 11.2, the build-in cache is deprecated. We still use it here, since explicit data source
        // initialization as performed here is not to be used in production systems anyway.
        oracleDataSource.setConnectionCachingEnabled(true);
        Properties cacheProps = new Properties();
        cacheProps.setProperty("MinLimit", "1");
        cacheProps.setProperty("MaxLimit", "8");
        cacheProps.setProperty("InitialLimit", "1");
        oracleDataSource.setConnectionCacheProperties(cacheProps);

        return oracleDataSource;
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
        if (pDataSource instanceof OracleDataSource) {
            final Properties connectionProperties = new Properties();
            connectionProperties.setProperty(OracleConnection.CONNECTION_PROPERTY_FIXED_STRING, "true");
            ((OracleDataSource) pDataSource).setConnectionProperties(connectionProperties);
            validationInfo.append("Connection parameters: ");
            validationInfo.append(";Data Source Implementation: ");
            validationInfo.append(pDataSource.getClass().getName());
            validationInfo.append(";URL: ");
            validationInfo.append(((OracleDataSource) pDataSource).getURL());
            validationInfo.append(";Username: ");
            validationInfo.append(((OracleDataSource) pDataSource).getUser());
        } else {
            // Check, whether we use an Oracle UCP data source
            if (pDataSource instanceof PoolDataSource) {
                final Properties connectionProperties = new Properties();
                connectionProperties.setProperty(OracleConnection.CONNECTION_PROPERTY_FIXED_STRING, "true");
                ((PoolDataSource) pDataSource).setConnectionProperties(connectionProperties);
                validationInfo.append("Connection parameters: ");
                validationInfo.append(";Data Source Implementation: ");
                validationInfo.append(pDataSource.getClass().getName());
                validationInfo.append(";URL: ");
                validationInfo.append(((PoolDataSource) pDataSource).getURL());
                validationInfo.append(";Username: ");
                validationInfo.append(((PoolDataSource) pDataSource).getUser());
            } else {
                validationInfo.append("Unable to retrieve connection parameters from data source. [");
                validationInfo.append(pDataSource.getClass().getName());
                validationInfo.append("]");
            }
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

        final String sequenceQuery = "SELECT ".concat(pSequenceName).concat(".NEXTVAL FROM DUAL");
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

        pPreparedStatement.executeBatch();
        return pPreparedStatement.getUpdateCount();
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

        pStmt.setObject(pParameterIndex, pValue);
    }

    /**
     * Binds a numeric array to a callable statement.
     *
     * @param pCon the JDBC connection
     * @param pCstmt the JDBC callable statement
     * @param pValue the value to bind (numeric array)
     * @param pParameterIndex the parameter index
     * @throws SQLException JDBC driver exception
     */
    @Override
    public void prepareNumericArray(final Connection pCon,
                                    final CallableStatement pCstmt,
                                    final Object pValue,
                                    final int pParameterIndex) throws SQLException {

        final ArrayDescriptor descriptor =
                ArrayDescriptor.createDescriptor(NOORM_ID_LIST_DB_TYPE_NAME, pCon);
        final ARRAY arrayToPass = new ARRAY(descriptor, pCon, pValue);
        ((OracleCallableStatement) pCstmt).setARRAY(pParameterIndex, arrayToPass);
    }

    /**
     * Provides database metadata for code generation and validation of generated code.
     *
     * @return the platform specific implementation of the metadata retrieval functionality
     */
    @Override
    public IMetadata getMetadata() {

        return oracleMetadata;
    }
}
