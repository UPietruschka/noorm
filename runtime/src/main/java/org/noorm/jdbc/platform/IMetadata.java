package org.noorm.jdbc.platform;

import java.sql.JDBCType;
import java.util.List;
import java.util.Map;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 11.02.14
 *         Time: 13:48
 */
public interface IMetadata {

    /**
     * Returns the list of table/column metadata accessible for the authenticated database user.
     *
     * @param pSchemaPattern a regular expression narrowing the set of schemas subject to metadata retrieval
     * @param pTableNamePattern a regular expression narrowing the set of tables subject to metadata retrieval
     * @return the requested
     */
    Map<String, List<TableMetadata>> findTableMetadata(final String pSchemaPattern, final String pTableNamePattern);

    /**
     * Resolves the JDBC datatype on basis of the platform specific type information found in JDBC metadata
     *
     * @param pDataType      the numeric data type code
     * @param pDataTypeName  the data type name
     * @param pDecimalDigits the number of decimal digits for non-integer like numeric types
     * @return the mapped JDBC standard type
     */
    JDBCType findJDBCType(final int pDataType, final String pDataTypeName, final int pDecimalDigits);

    /**
     * Returns the list of packages of stored procedures subject to Java code generation.
     *
     * @param pSearchRegex a regular expression to filter the list of packages
     * @return the list of packages
     */
    List<String> findPackageNames(final String pSearchRegex);

    /**
     * Returns the list of procedures contained in the given package.
     *
     * @param pPackageName the package name
     * @return the list of procedures
     */
    List<String> findProcedureNames(final String pPackageName);

    /**
     * Returns all accessible sequences.
     *
     * @return the list of sequence names
     */
    List<Sequence> findSequences();

    /**
     * Returns all primary key columns.
     *
     * @param pTableName the table name
     * @return the list of primary key columns for the given table
     */
    List<PrimaryKeyColumn> findPkColumns(final String pTableName);

    /**
     * Returns the parameters for a given stored procedure.
     *
     * @param pPackageName the package name
     * @param pProcedureName the procedure name
     * @return the list of parameters for the given procedure
     */
    List<Parameter> findProcedureParameters(final String pPackageName, final String pProcedureName);

    /**
     * Returns the hash value for the source code of a given stored procedure package.
     *
     * @param pPackageName the package name
     * @return the hash value
     */
    String getPackageHashValue(final String pPackageName);

    /**
     * Returns the type for a given parameter of a stored procedure.
     *
     * @param pPackageName the package name
     * @param pProcedureName the procedure name
     * @param pParameterName the parameter name
     * @return the type
     */
    String getParameterRowtype(final String pPackageName,
                               final String pProcedureName,
                               final String pParameterName);

    /**
     * Returns metadata for a database type definition.
     *
     * @return the list of record metadata
     */
    Map<String, List<TableMetadata>> findRecordMetadata();
}
