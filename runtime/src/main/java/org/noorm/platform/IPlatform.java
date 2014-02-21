package org.noorm.platform;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@ext.secunet.com
 *         Date: 11.02.14
 *         Time: 13:48
 *         <p/>
 *         Project class implementation
 */
public interface IPlatform {

    /**
     * Creates a platform specific data source.

     * @param pURL the connection URL
     * @param pUsername the username
     * @param pPassword the password
     * @return the established data source
     */
    DataSource getDataSource(final String pURL, final String pUsername, final String pPassword) throws SQLException;

    /**
     * Returns the platform specific query to retrieve a sequence value generated by the database.
     *
     * @param pSequenceName the name of the database sequence
     * @return the SELECT statement to retrieve a single sequence value for the given platform
     */
    String getSequenceQuery(final String pSequenceName);

    /**
     * Executes a batch over a prepared statement.
     * Different databases and JDBC drivers handle the update count differently. To get a reliable update,
     * a platform specific implementation is required.
     *
     * @param pPreparedStatement the prepared statement ready for executing the next batch
     * @return the reliable update count for the platform in use
     */
    int executeBatchWithReliableCount(final PreparedStatement pPreparedStatement) throws SQLException;

    /**
     * Sets an object value for an DML statement (INSERT, UPDATE, DELETE).
     *
     * @param pStmt the prepared SQL statement
     * @param pValue the parameter value to be set
     * @param pParameterIndex the index of the parameter
     * @param pSQLType the SQL type. Usually one type specified in java.sql.Types or a proprietary type
     * @throws SQLException
     */
    void setObject(final PreparedStatement pStmt,
                   final Object pValue,
                   final int pParameterIndex,
                   final int pSQLType) throws SQLException;

    /**
     * Provides database metadata for code generation and validation of generated code.
     *
     * @return the platform specific implementation of the metadata retrieval functionality
     */
    IMetadata getMetadata();
}
